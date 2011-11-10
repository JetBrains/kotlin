package org.jetbrains.jet.lang.psi;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author Stepan Koltsov
 */
public class JetPsiUtilTest extends TestCase {
    
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

}
