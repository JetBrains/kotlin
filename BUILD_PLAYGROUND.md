# Build Playground Branch

Q: Why do we need to have `4u7/build-playground` branch in `kotlin-ultimate` repo?

A: This is a special branch that is used for testing correctness of Kotlin projects and build configurations running at [TeamCity server](https://teamcity.jetbrains.com/).
Such projects/build configurations are defined in the form of TeamCity Kotlin DSL. Their source code is stored in https://github.com/JetBrains/kotlin-teamcity-build repo.

There is a staging project at TeamCity named [Kotlin Build Playground](https://teamcity.jetbrains.com/project.html?projectId=Kotlin_playground&tab=projectOverview).
This project has almost the same build configuration as standard Kotlin projects such as "Kotlin Dev" or "1.3.30".
And is used for testing of potentially breaking changes:
1. Make changes to `playground` branch of `kotlin-teamcity-build` project. Commit. Push.
2. Manually run "Aggregate Builds" build configuration at TeamCity. This configuration will attempt to build and run IDE tests the whole Kotlin project using 2 VCS roots:
   - `4u7/build-playground` branch of `kotlin` repo
   - and `4u7/build-playground` branch of `kotlin-ultimate` repo
3. Check if it works properly.
4. If necessary, re-iterate with #1.
5. Merge changes to `master` branch of `kotlin-teamcity-build` project. Commit. Push.

Since #5 the changes will be applied to all new runs of "Kotlin Dev" project.
