/*
 * Copyright (c) 2005, Sam Pullara. All Rights Reserved.
 * You may modify and redistribute as long as this attribution remains.
 */

package org.jetbrains.kotlin.cli.common.parser.com.sampullara.cli;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Argument {
    /**
     * This is the actual command line argument itself
     */
    String value() default "";
    /**
     * If this is true, then the argument must be set or the parse will fail
     */
    boolean required() default false;

    /**
     * This is the prefix expected for the argument
     */
    String prefix() default "-";
    /**
     * Each argument can have an alias
     */
    String alias() default "";
    /**
     * A description of the argument that will appear in the usage method
     */
    String description() default "";

    /**
     * A delimiter for arguments that are multi-valued.
     */
    String delimiter() default ",";
}
