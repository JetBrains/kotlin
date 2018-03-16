/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure

import java.io.File
import java.io.PrintWriter
import java.util.*
import kotlin.concurrent.schedule

object Report {

    private val _log_file: PrintWriter by lazy {
        /*val f = */File("_LOG_.txt").printWriter()
//        Timer().schedule(10000) {
//            _log_file.close()
//        }
//        f
    }

    public fun log(debugString: String, classs: String) {
        "[$classs] : $debugString".let {
//            _log_file.println(it)
            println(it)
        }
    }
}