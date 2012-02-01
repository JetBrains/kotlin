package org.jetbrains.jet.lang.psi;

/**
 * User: Alefas
 * Date: 31.01.12
 */

/**
 * This is an interface to show that {@link JetExpression} is not
 * actually an expression (in meaning that this expression can be placed after "val x = ").
 * This is possibly redundant interface, all inheritors of this interface should be refactored that they are not
 * {@link JetExpression}, after such refactoring, this interface can be removed.
 */
public interface JetStatementExpression {
}
