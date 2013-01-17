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

package org.jetbrains.jet.lang.psi;

import com.intellij.psi.util.PsiTreeUtil;
import junit.framework.Assert;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

public class JetPsiUtilTest extends JetLiteFixture {
    
    public void testUnquotedIdentifier() {
        Assert.assertEquals("", JetPsiUtil.unquoteIdentifier(""));
        Assert.assertEquals("a2", JetPsiUtil.unquoteIdentifier("a2"));
        Assert.assertEquals("", JetPsiUtil.unquoteIdentifier("``"));
        Assert.assertEquals("a2", JetPsiUtil.unquoteIdentifier("`a2`"));
    }

    public void testUnquotedIdentifierOrFieldReference() {
        Assert.assertEquals("", JetPsiUtil.unquoteIdentifierOrFieldReference(""));
        Assert.assertEquals("a2", JetPsiUtil.unquoteIdentifierOrFieldReference("a2"));
        Assert.assertEquals("", JetPsiUtil.unquoteIdentifierOrFieldReference("``"));
        Assert.assertEquals("a2", JetPsiUtil.unquoteIdentifierOrFieldReference("`a2`"));
        Assert.assertEquals("$a2", JetPsiUtil.unquoteIdentifierOrFieldReference("$a2"));
        Assert.assertEquals("$a2", JetPsiUtil.unquoteIdentifierOrFieldReference("$`a2`"));
    }

    public void testConvertToImportPath() {
        Assert.assertEquals(null, getImportPathFromParsed("import "));
        Assert.assertEquals(null, getImportPathFromParsed("import some."));
        Assert.assertEquals(null, getImportPathFromParsed("import *"));
        Assert.assertEquals(null, getImportPathFromParsed("import some.test.* as SomeTest"));
        Assert.assertEquals(new ImportPath(new FqName("some"), false), getImportPathFromParsed("import some?.Test"));

        Assert.assertEquals(new ImportPath(new FqName("some"), false), getImportPathFromParsed("import some"));
        Assert.assertEquals(new ImportPath(new FqName("some"), true), getImportPathFromParsed("import some.*"));
        Assert.assertEquals(new ImportPath(new FqName("some.Test"), false), getImportPathFromParsed("import some.Test"));
        Assert.assertEquals(new ImportPath(new FqName("some.test"), true), getImportPathFromParsed("import some.test.*"));
        Assert.assertEquals(new ImportPath(new FqName("some.test"), false, Name.identifier("SomeTest")), getImportPathFromParsed("import some.test as SomeTest"));

        Assert.assertEquals(new ImportPath(new FqName("some.Test"), false), getImportPathFromParsed("import some.\nTest"));
        Assert.assertEquals(new ImportPath(new FqName("some.Test"), false), getImportPathFromParsed("import some./* hello world */Test"));
        Assert.assertEquals(new ImportPath(new FqName("some.Test"), false), getImportPathFromParsed("import some.    Test"));

        Assert.assertNotSame(new ImportPath(new FqName("some.test"), false), getImportPathFromParsed("import some.Test"));
    }

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return new JetCoreEnvironment(getTestRootDisposable(), new CompilerConfiguration());
    }

    private ImportPath getImportPathFromParsed(String text) {
        JetImportDirective importDirective =
                PsiTreeUtil.findChildOfType(JetPsiFactory.createFile(getProject(), text), JetImportDirective.class);

        assertNotNull("At least one import directive is expected", importDirective);

        return JetPsiUtil.getImportPath(importDirective);
    }
}
