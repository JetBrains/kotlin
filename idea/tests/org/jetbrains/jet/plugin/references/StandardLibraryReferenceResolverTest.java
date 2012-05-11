/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.references;

import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorVisitor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Evgeny Gerashchenko
 * @since 5/11/12
 */
public class StandardLibraryReferenceResolverTest extends LightCodeInsightTestCase {
    public void testAllReferencesResolved() {
        StandardLibraryReferenceResolver referenceResolver = getProject().getComponent(StandardLibraryReferenceResolver.class);
        for (DeclarationDescriptor descriptor : getAllStandardDescriptors(JetStandardClasses.STANDARD_CLASSES_NAMESPACE)) {
            if (descriptor instanceof NamespaceDescriptor && "jet".equals(descriptor.getName())) continue;
            assertNotNull("Can't resolve " + descriptor, referenceResolver.resolveStandardLibrarySymbol(descriptor));
        }
    }

    private static Collection<DeclarationDescriptor> getAllStandardDescriptors(DeclarationDescriptor baseDescriptor) {
        final ArrayList<DeclarationDescriptor> descriptors = new ArrayList<DeclarationDescriptor>();
        baseDescriptor.acceptVoid(new DeclarationDescriptorVisitor<Void, Void>() {
            private Void visitDescriptors(Collection<? extends DeclarationDescriptor> descriptors) {
                for (DeclarationDescriptor descriptor : descriptors) {
                    descriptor.acceptVoid(this);
                }
                return null;
            }

            @Override
            public Void visitClassDescriptor(ClassDescriptor descriptor, Void data) {
                descriptors.add(descriptor);
                return visitDescriptors(descriptor.getDefaultType().getMemberScope().getAllDescriptors());
            }

            @Override
            public Void visitNamespaceDescriptor(NamespaceDescriptor descriptor, Void data) {
                descriptors.add(descriptor);
                return visitDescriptors(descriptor.getMemberScope().getAllDescriptors());
            }

            @Override
            public Void visitDeclarationDescriptor(DeclarationDescriptor descriptor, Void data) {
                descriptors.add(descriptor);
                return null;
            }
        });
        return descriptors;
    }
}
