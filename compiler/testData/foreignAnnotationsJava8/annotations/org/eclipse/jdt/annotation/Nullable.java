/*******************************************************************************
 * Copyright (c) 2011, 2013 Stephan Herrmann and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *     IBM Corporation - bug fixes
 *******************************************************************************/
package org.eclipse.jdt.annotation;

import static java.lang.annotation.ElementType.TYPE_USE;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifier for a reference type in a {@link ElementType#TYPE_USE TYPE_USE} position:
 * The type that has this annotation explicitly includes the value <code>null</code>.
 * <p>
 * If annotation based null analysis is enabled using this annotation has two consequences:
 * </p>
 * <ol>
 * <li>Binding a <code>null</code> value to an entity (field, local variable, method parameter or method return value)
 *     of this type is legal.</li>
 * <li>Dereferencing an expression of this type is unsafe, i.e., a <code>NullPointerException</code> can occur at runtime.</li>
 * </ol>
 * <p>
 * <b>Note:</b> Since org.eclipse.jdt.annotation 2.0.0, the
 * <code>@Target</code> is <code>{TYPE_USE}</code>. For the old API, see
 * <a href="http://help.eclipse.org/kepler/topic/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/annotation/Nullable.html">
 * <code>@Nullable</code> in 1.1.0</a>.
 * </p>
 * @since 1.0
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ TYPE_USE })
public @interface Nullable {
    // marker annotation with no members
}