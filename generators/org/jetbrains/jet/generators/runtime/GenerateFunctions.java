/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.generators.runtime;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.utils.ExceptionUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

public class GenerateFunctions {
    public static final int MAX_PARAM_COUNT = 22;
    public static final File JET_SRC_DIR = new File("compiler/frontend/src/jet/");
    public static final File RUNTIME_SRC_DIR = new File("runtime/src/jet/");

    private final PrintWriter out;
    private final FunctionKind kind;

    private GenerateFunctions(PrintWriter out, FunctionKind kind) {
        this.out = out;
        this.kind = kind;
    }

    private enum FunctionKind {
        FUNCTION("Function", false, null),
        EXTENSION_FUNCTION("ExtensionFunction", true, null),

        K_FUNCTION("KFunction", false, "Function"),
        K_MEMBER_FUNCTION("KMemberFunction", true, "ExtensionFunction"),
        K_EXTENSION_FUNCTION("KExtensionFunction", true, "ExtensionFunction");

        private final String classNamePrefix;
        private final boolean hasReceiverParameter;
        private final String superClassNamePrefix;

        private FunctionKind(String classNamePrefix, boolean hasReceiverParameter, String superClassNamePrefix) {
            this.classNamePrefix = classNamePrefix;
            this.hasReceiverParameter = hasReceiverParameter;
            this.superClassNamePrefix = superClassNamePrefix;
        }

        public String getJetFileName() {
            return classNamePrefix + "s.jet";
        }

        public String getClassName(int i) {
            return classNamePrefix + i;
        }

        public String getImplClassName(int i) {
            return classNamePrefix + "Impl" + i;
        }

        @Nullable
        public String getSuperClassName(int i) {
            return superClassNamePrefix != null ? superClassNamePrefix + i : null;
        }
    }

    private void generateBuiltInFunctions() {
        generated();
        for (int i = 0; i <= MAX_PARAM_COUNT; i++) {
            out.print("public trait " + kind.getClassName(i));
            generateTypeParameters(i, true);
            generateSuperClass(i, true);
            generateFunctionClassBody(i, true);
        }
    }

    private void generateSuperClass(int i, boolean kotlin) {
        String name = kind.getSuperClassName(i);
        if (name == null) return;

        out.print(kotlin ? " : " : " extends ");
        out.print(name);
        generateTypeParameters(i, false);
    }

    private void generateFunctionClassBody(int i, boolean kotlin) {
        switch (kind) {
            case FUNCTION:
            case EXTENSION_FUNCTION: {
                out.println(" {");
                if (kotlin) {
                    generateKotlinInvokeSignature(i);
                } else {
                    generateJavaInvokeSignature(i);
                }
                out.println("}");
                break;
            }

            case K_FUNCTION:
            case K_MEMBER_FUNCTION:
            case K_EXTENSION_FUNCTION: {
                if (kotlin) {
                    out.println();
                }
                else {
                    out.println(" {");
                    out.println("}");
                }
                break;
            }

            default:
                throw new IllegalStateException("Unknown kind: " + kind);
        }
    }

    private void generateKotlinInvokeSignature(int i) {
        out.print("    public fun " + (kind.hasReceiverParameter ? "T." : "") + "invoke(");
        for (int j = 1; j <= i; j++) {
            out.print("p" + j + ": " + "P" + j);
            if (j < i) {
                out.print(", ");
            }
        }
        out.println(") : R");
    }

    private void generateRuntimeFunction(int i) {
        generateRuntimeClassHeader();

        out.println("import org.jetbrains.jet.rt.annotation.AssertInvisibleInResolver;");
        out.println();
        out.println("@AssertInvisibleInResolver");

        out.print("public interface " + kind.getClassName(i));
        generateTypeParameters(i, false);
        generateSuperClass(i, false);
        generateFunctionClassBody(i, false);
    }

    private void generateJavaInvokeSignature(int i) {
        out.print("    R invoke(");
        if (kind.hasReceiverParameter) {
            out.print("T receiver");
            if (i > 0) {
                out.print(", ");
            }
        }
        for (int j = 1; j <= i; j++) {
            out.print("P" + j + " p" + j);
            if (j < i) {
                out.print(", ");
            }
        }
        out.println(");");
    }

    private void generateTypeParameters(int i, boolean kotlinVariance) {
        out.print("<");
        if (kind.hasReceiverParameter) {
            if (kotlinVariance) out.print("in ");
            out.print("T, ");
        }
        for (int j = 1; j <= i; j++) {
            if (kotlinVariance) out.print("in ");
            out.print("P" + j + ", ");
        }
        if (kotlinVariance) out.print("out ");
        out.print("R>");
    }

    private void generateRuntimeFunctionImpl(int i) {
        generateRuntimeClassHeader();

        out.print("public abstract class " + kind.getImplClassName(i));
        generateTypeParameters(i, false);
        out.print(" extends DefaultJetObject");
        out.print(" implements " + kind.getClassName(i));
        generateTypeParameters(i, false);
        out.println(" {");
        generateToStringForFunctionImpl();
        out.println("}");
    }

    private void generateToStringForFunctionImpl() {
        out.println("    @Override");
        out.println("    public String toString() {");
        out.println("        return getClass().getGenericSuperclass().toString();");
        out.println("    }");
    }

    private void generateRuntimeClassHeader() {
        try {
            out.println(FileUtil.loadFile(new File("injector-generator/copyright.txt")));
        }
        catch (IOException e) {
            ExceptionUtils.rethrow(e);
        }
        out.println("package jet;");
        out.println();
    }

    private void generated() {
        out.println("// Generated by " + GenerateFunctions.class.getName());
        out.println();
        out.println("package jet");
        out.println();
    }

    public static void main(String[] args) throws FileNotFoundException {
        assert JET_SRC_DIR.exists() : "jet.* src dir does not exist: " + JET_SRC_DIR.getAbsolutePath();
        assert RUNTIME_SRC_DIR.exists() : "Runtime src dir does not exist: " + RUNTIME_SRC_DIR.getAbsolutePath();

        for (FunctionKind kind : FunctionKind.values()) {
            PrintWriter functions = new PrintWriter(new File(JET_SRC_DIR, kind.getJetFileName()));
            new GenerateFunctions(functions, kind).generateBuiltInFunctions();
            functions.close();
            
            for (int i = 0; i <= MAX_PARAM_COUNT; i++) {
                PrintWriter function = new PrintWriter(new File(RUNTIME_SRC_DIR, kind.getClassName(i) + ".java"));
                new GenerateFunctions(function, kind).generateRuntimeFunction(i);
                function.close();

                PrintWriter functionImpl = new PrintWriter(new File(RUNTIME_SRC_DIR, kind.getImplClassName(i) + ".java"));
                new GenerateFunctions(functionImpl, kind).generateRuntimeFunctionImpl(i);
                functionImpl.close();
            }
        }
    }
}
