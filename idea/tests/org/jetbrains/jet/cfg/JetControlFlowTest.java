/*
 * @author max
 */
package org.jetbrains.jet.cfg;

import com.intellij.openapi.util.io.FileUtil;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBase;
import org.jetbrains.jet.lang.cfg.pseudocode.Instruction;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.cfg.pseudocode.JetPseudocodeTrace;
import org.jetbrains.jet.lang.cfg.pseudocode.Pseudocode;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamedDeclaration;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class JetControlFlowTest extends JetTestCaseBase {
    static {
        System.setProperty("idea.platform.prefix", "Idea");
    }

    public JetControlFlowTest(String dataPath, String name) {
        super(dataPath, name);
    }

    @Override
    protected void runTest() throws Throwable {
        configureByFile(getTestFilePath());
        JetFile file = (JetFile) getFile();

        final Map<JetElement, Pseudocode> data = new LinkedHashMap<JetElement, Pseudocode>();
        final JetPseudocodeTrace pseudocodeTrace = new JetPseudocodeTrace() {

            @Override
            public void recordControlFlowData(@NotNull JetElement element, @NotNull Pseudocode pseudocode) {
                data.put(element, pseudocode);
            }

            @Override
            public void close() {
                for (Pseudocode pseudocode : data.values()) {
                    pseudocode.postProcess();
                }
            }

            @Override
            public void recordRepresentativeInstruction(@NotNull JetElement element, @NotNull Instruction instruction) {
            }

        };

        AnalyzingUtils.analyzeNamespace(file.getRootNamespace(), new JetControlFlowDataTraceFactory() {
            @NotNull
            @Override
            public JetPseudocodeTrace createTrace(JetElement element) {
                return pseudocodeTrace;
            }
        });

        try {
            processCFData(name, data);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            if ("true".equals(System.getProperty("jet.control.flow.test.dump.graphs"))) {
                dumpDot(name, data.values());
            }
        }
    }

    private void processCFData(String name, Map<JetElement, Pseudocode> data) throws IOException {
        Collection<Pseudocode> pseudocodes = data.values();

        StringBuilder instructionDump = new StringBuilder();
        int i = 0;
        for (Pseudocode pseudocode : pseudocodes) {
            JetElement correspondingElement = pseudocode.getCorrespondingElement();
            String label;
            if (correspondingElement instanceof JetNamedDeclaration) {
                JetNamedDeclaration namedDeclaration = (JetNamedDeclaration) correspondingElement;
                label = namedDeclaration.getName();
            }
            else {
                label = "anonymous_" + i++;
            }

            instructionDump.append("== ").append(label).append(" ==\n");

            instructionDump.append(correspondingElement.getText());
            instructionDump.append("\n---------------------\n");
            pseudocode.dumpInstructions(instructionDump);
            instructionDump.append("=====================\n");
        }

        String expectedInstructionsFileName = getTestDataPath() + "/" + getTestFilePath().replace(".jet", ".instructions");
        File expectedInstructionsFile = new File(expectedInstructionsFileName);
        if (!expectedInstructionsFile.exists()) {
            FileUtil.writeToFile(expectedInstructionsFile, instructionDump.toString());
            fail("No expected instructions for " + name + " generated result is written into " + expectedInstructionsFileName);
        }
        String expectedInstructions = FileUtil.loadFile(expectedInstructionsFile);

        assertEquals(expectedInstructions, instructionDump.toString());

//                        StringBuilder graphDump = new StringBuilder();
//                        for (Pseudocode pseudocode : pseudocodes) {
//                            topOrderDump(pseudocode.)
//                        }
    }

    private void dumpDot(String name, Collection<Pseudocode> pseudocodes) throws FileNotFoundException {
        String graphFileName = getTestDataPath() + "/" + getTestFilePath().replace(".jet", ".dot");
        File target = new File(graphFileName);

        PrintStream out = new PrintStream(target);

        out.println("digraph " + name + " {");
        int[] count = new int[1];
        Map<Instruction, String> nodeToName = new HashMap<Instruction, String>();
        for (Pseudocode pseudocode : pseudocodes) {
            pseudocode.dumpNodes(out, count, nodeToName);
        }
        int i = 0;
        for (Pseudocode pseudocode : pseudocodes) {
            String label;
            JetElement correspondingElement = pseudocode.getCorrespondingElement();
            if (correspondingElement instanceof JetNamedDeclaration) {
                JetNamedDeclaration namedDeclaration = (JetNamedDeclaration) correspondingElement;
                label = namedDeclaration.getName();
            }
            else {
                label = "anonymous_" + i;
            }
            out.println("subgraph cluster_" + i + " {\n" +
                        "label=\"" + label + "\";\n" +
                        "color=blue;\n");
            pseudocode.dumpEdges(out, count, nodeToName);
            out.println("}");
            i++;
        }
        out.println("}");
        out.close();
    }

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JetTestCaseBase.suiteForDirectory(getTestDataPathBase(), "/cfg/", true, new JetTestCaseBase.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name) {
                return new JetControlFlowTest(dataPath, name);
            }
        }));
        return suite;
    }

}
