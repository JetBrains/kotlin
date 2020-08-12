job("GitHub PRs => Space MRs") {
  container("openjdk:11.0.3-jdk") {
    kotlinScript { api ->
      if (api.gitBranch().startsWith("refs/heads/pull/")) {
        val pullRequestNumber = api.gitBranch()
          .splitToSequence("/")
          .mapNotNull { it.toIntOrNull() }
          .single()
        val pullRequestUrl = "https://github.com/JetBrains/intellij-kotlin/pull/$pullRequestNumber"
        println("Creating Merge Request for $pullRequestUrl")
        api.space().projects
          .repositories.mergeRequests
          .createMergeRequest(
            title = pullRequestUrl,
            sourceBranch = api.gitBranch(),
            targetBranch = System.getProperty("space.system.branch"),
            project = api.projectIdentifier(),
            repository = System.getProperty("space.system.repository")
          )
      }
    }
  }
}
