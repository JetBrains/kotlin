job("Prepare warmup data") {
    startOn {
        // run on schedule every day at 5AM
        schedule {
            cron("0 5 * * *")
        }
    }

    git {
        // fetch the entire commit history
        depth = UNLIMITED_DEPTH
    }

    warmup(ide = Ide.Fleet, profileId = "fleet") {
        requirements {
            workerTags("fleet")
        }
        scriptLocation = "./.space/warmup.sh"
    }
}
