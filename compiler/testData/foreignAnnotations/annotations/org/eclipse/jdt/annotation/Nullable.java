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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifier for a type in a method signature or a local variable declaration:
 * The entity (return value, parameter, field, local variable) whose type has this
 * annotation is allowed to have the value <code>null</code> at runtime.
 * <p>
 * This has two consequences:
 * <ul>
 * <li>Binding a <code>null</code> value to the entity is legal.</li>
 * <li>Dereferencing the entity is unsafe, i.e., a <code>NullPointerException</code> can occur at runtime.</li>
 * </ul>
 * </p>
 * @since 1.0
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ FIELD, METHOD, PARAMETER, LOCAL_VARIABLE })
public @interface Nullable {
    // marker annotation with no members
}