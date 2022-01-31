import circlet.pipelines.config.dsl.api.Ide

fun setupWarmupJob(ide: Ide) {
    job("Warmup ${ide.name}") {
        startOn {
            // run on schedule every day at 5AM
            schedule { cron("0 5 * * *") }
        }

        warmup(ide) {
            scriptLocation = "./.devenv/warmup.sh"
        }

        git {
            // fetch the entire commit history
            depth = UNLIMITED_DEPTH
            // fetch all branches
            refSpec = "refs/*:refs/*"
        }
    }
}

setupWarmupJob(Ide.Idea)
setupWarmupJob(Ide.Fleet)
