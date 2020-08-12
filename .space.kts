job("GitHub PRs => Space MRs") {
  container("openjdk:11.0.3-jdk") {
    kotlinScript { api ->
      if (api.gitBranch().startsWith("refs/heads/pull/")) {
        fun String.parameterValue() = api.parameters[this] ?: error("Parameter $this is undefined")
        val pullRequestNumber = api.gitBranch()
          .splitToSequence("/")
          .mapNotNull { it.toIntOrNull() }
          .single()
        val pullRequestUrl = "https://github.com/JetBrains/intellij-kotlin/pull/$pullRequestNumber"
        val project = api.projectIdentifier()
        val sourceBranch = api.gitBranch()
        val targetBranch = "space.system.branch".parameterValue()
        val repository = "space.system.repository".parameterValue()
        println("""
          Creating Merge Request for $pullRequestUrl:
            source branch: $sourceBranch
            target branch: $targetBranch
            project: $project
            repository: $repository
        """.trimIndent())
        api.space().projects
          .repositories.mergeRequests
          .createMergeRequest(
            title = pullRequestUrl,
            sourceBranch = sourceBranch, targetBranch = targetBranch,
            project = project, repository = repository
          )
      }
    }
  }
}
