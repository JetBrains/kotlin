/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import org.gradle.api.Project
import java.net.URI

var Project.bootstrapKotlinVersion: String
    get() = property("bootstrapKotlinVersion") as String
    private set(value) {
        extensions.extraProperties.set("bootstrapKotlinVersion", value)
    }

var Project.bootstrapKotlinRepo: String?
    get() = property("bootstrapKotlinRepo") as String?
    private set(value) {
        extensions.extraProperties.set("bootstrapKotlinRepo", value)
    }
