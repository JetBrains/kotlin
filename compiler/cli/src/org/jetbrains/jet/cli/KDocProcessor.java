/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.jet.cli;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.compiler.JetCoreEnvironment;
import org.jetbrains.jet.compiler.JetFileProcessor;
import org.jetbrains.jet.lang.Configuration;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.java.JavaBridgeConfiguration;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 */
public class KDocProcessor implements JetFileProcessor {

    private final String outputDir;

    public KDocProcessor(String outputDir) {
        this.outputDir = outputDir;
    }

    @Override
    public void processFiles(List<JetFile> sources) {
        // JetFile's are PSI (Program Source Interface) classes, i.e. they contain the concrete syntax trees of files
        if (sources.isEmpty()) return;

        // Let's perform the semantic analysis
        Project project = sources.get(0).getProject();
        Configuration javaBridgeConfiguration = JavaBridgeConfiguration.createJavaBridgeConfiguration(project, new BindingTraceContext(), Configuration.EMPTY);

        // The BindingContext holds a mapping between the semantic data (DesclarationDescriptor's) and PSI
        BindingContext context = AnalyzingUtils.analyzeFiles(project, javaBridgeConfiguration, sources, Predicates.<PsiFile>alwaysFalse(), JetControlFlowDataTraceFactory.EMPTY);

        // Now we need all the packages. They are listed in the files
        Set<NamespaceDescriptor> allNamespaces = Sets.newHashSet();
        for (JetFile source : sources) {
            // We retrieve a descriptor by a PSI element from the context
            NamespaceDescriptor namespaceDescriptor = context.get(BindingContext.NAMESPACE, source);
            allNamespaces.add(namespaceDescriptor);
        }

        for (NamespaceDescriptor namespace : allNamespaces) {
            // Let's take all the declarations in the namespace...
            processDescriptors(namespace.getMemberScope().getAllDescriptors(), context);
        }

        // TODO fire up the KDoc processor here...
    }

    private void processDescriptors(Collection<DeclarationDescriptor> allDescriptors, BindingContext context) {
        for (DeclarationDescriptor descriptor : allDescriptors) {
            PsiElement classElement = context.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor);
            // Print the doc comment text
            String docComment = getDocCommentFor(classElement);
            if (docComment != null) {
                System.out.println("Docs for " + descriptor.getName() + ": " + docComment);
            }
            else {
                System.out.println("No docs for " + descriptor.getName());
            }
            // Print the class header (verbose)
            System.out.println(DescriptorRenderer.TEXT.render(descriptor));
            // Process members, if any
            if (descriptor instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
                processDescriptors(classDescriptor.getDefaultType().getMemberScope().getAllDescriptors(), context);
            }
        }
    }

    @Nullable
    private String getDocCommentFor(PsiElement psiElement) {
        // This method is a hack. Doc comments should be easilty accessible, but they aren't for now.
        ASTNode node = psiElement.getNode().getTreePrev();
        while (node != null && (node.getElementType() == JetTokens.WHITE_SPACE || node.getElementType() == JetTokens.BLOCK_COMMENT)) {
            node = node.getTreePrev();
        }
        if (node == null) return null;
        if (node.getElementType() != JetTokens.DOC_COMMENT) return null;
        return node.getText();
    }

    public static void main(String[] args) {
        JetCoreEnvironment jetCoreEnvironment = new JetCoreEnvironment(new Disposable() {
            @Override
            public void dispose() {
            }
        });
        List<JetFile> files = Lists.newArrayList();
        files.add(JetPsiFactory.createFile(jetCoreEnvironment.getProject(), "package a;/**sdfsdf*/class A {fun foo() {} /**doc*/ fun bar() {}}"));
        new KDocProcessor("").processFiles(files);
    }
}
