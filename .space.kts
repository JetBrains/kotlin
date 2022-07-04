/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun warmupJob(ide: Ide, devfilePath: String) {
    job("Kotlin project warmup for ${ide.name}") {
        startOn {
            schedule { cron("0 2 * * *") }  // 5 am GMT +3
        }

        warmup(ide = ide) {
            scriptLocation = "./dev-env-warmup.sh"
            devfile = devfilePath
        }

        git {
            // fetch the entire commit history
            depth = UNLIMITED_DEPTH
            // fetch all branches
            refSpec = "refs/*:refs/*"
        }
    }
}

warmupJob(Ide.Idea, ".space/idea.devfile.yaml")
warmupJob(Ide.Fleet, ".space/fleet.devfile.yaml")