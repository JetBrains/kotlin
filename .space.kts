/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import circlet.pipelines.config.dsl.api.Ide

fun warmupJob(ide: Ide) {
    job("Kotlin project warmup for ${ide.name}") {
        startOn {
            schedule { cron("0 5 * * *") }
        }

        warmup(ide = ide) {
            scriptLocation = "./dev-env-warmup.sh"
        }

        git {
            // fetch the entire commit history
            depth = UNLIMITED_DEPTH
            // fetch all branches
            refSpec = "refs/*:refs/*"
        }
    }
}

warmupJob(Ide.Idea)
warmupJob(Ide.Fleet)