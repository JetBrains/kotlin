/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.annotations;

import java.lang.annotation.*;

/**
 * This is a copy of `org.jetbrains.annotations.Nls` from IntelliJ. It's need in the build because Kotlin depends on annotations-13.0,
 * an older version where Nls doesn't have `capitalization`, whereas new versions of the IntelliJ platform reference it in the bytecode.
 * Since we intentionally remove the newer annotations artifact from the intellij-core dependency and use version 13.0 in the build,
 * javac wouldn't find the class file Nls$Capitalization referenced in the bytecode, and would report a non-suppressible warning
 * for each usage site of it in IntelliJ binaries.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE, ElementType.PACKAGE})
public @interface Nls {
    Capitalization capitalization() default Capitalization.NotSpecified;

    enum Capitalization { NotSpecified, Title, Sentence }
}
