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
package org.jetbrains.jet.jvm.compiler;

import junit.framework.Assert;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptorForObject;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.ErrorUtils;

import java.io.File;
import java.util.Collection;

import static org.jetbrains.jet.jvm.compiler.LoadDescriptorUtil.TEST_PACKAGE_FQNAME;
import static org.jetbrains.jet.lang.resolve.BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR;

public final class CompileKotlinAgainstBinariesCustomTest extends AbstractCompileKotlinAgainstCustomBinariesTest {

    public void testDuplicateObjectInSourcesAndBinaries() throws Exception {
        BindingContext context = analyzeFile(new File(
                "compiler/testData/compileKotlinAgainstBinariesCustom/duplicateObjectInBinaryAndSources/duplicateObjectInBinaryAndSources.kt"));
        NamespaceDescriptor namespaceDescriptor = context.get(FQNAME_TO_NAMESPACE_DESCRIPTOR, TEST_PACKAGE_FQNAME);
        assert namespaceDescriptor != null;
        Collection<DeclarationDescriptor> allDescriptors = namespaceDescriptor.getMemberScope().getAllDescriptors();
        Assert.assertEquals(allDescriptors.size(), 2);
        for (DeclarationDescriptor descriptor : allDescriptors) {
            Assert.assertTrue(descriptor.getName().asString().equals("Lol"));
            Assert.assertTrue(descriptor instanceof VariableDescriptorForObject);
            Assert.assertFalse("Object property should have valid class", ErrorUtils.isError(((VariableDescriptorForObject) descriptor).getObjectClass()));
        }
    }
}
