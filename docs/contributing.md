# What this repository is

This repository contains sources of:
1. Kotlin compiler: frontend and jvm, js, wasm and native backends.
2. Plugins extending compiler functionality.
3. Kotlin standard library and others including
    * kotlin-test
    * kotlinx-metadata
    * kotlin-reflect 
4. Build systems support: Gradle (incl. Dukat), Maven, JPS and Ant. 
5. Kotlin scripting support.

# What this repository is not

There are other activities around the language residing in different repositories.

* Kotlin language support in [IntelliJ Kotlin plugin](https://plugins.jetbrains.com/plugin/6954-kotlin) comprising the compiler and IDE specifics (code highlighting,
intentions, inspections, refactorings, etc.).\
Starting from IntelliJ version `2021.2` its code is a part of
[JetBrains/intellij](https://github.com/JetBrains/intellij-community) repository.\
Platforms `2021.1` and earlier get plugin built from [JetBrains/intellij-kotlin](https://github.com/JetBrains/intellij-kotlin).

# Contributing

We love contributions! There is [a lot to do on Kotlin](https://youtrack.jetbrains.com/issues/KT) and on the
[standard library](https://youtrack.jetbrains.com/issues/KT?q=%23Kotlin%20%23Unresolved%20and%20(links:%20KT-2554,%20KT-4089%20or%20%23Libraries)) so why not chat with us
about what you're interested in doing? Please join the #kontributors channel in [our Slack chat](http://slack.kotlinlang.org/)
and let us know about your plans.

If you want to find some issues to start off with, try [this query](https://youtrack.jetbrains.com/issues/KT?q=tag:%20%7BUp%20For%20Grabs%7D%20and%20State:%20Open) which should find all open Kotlin issues that are marked as "up-for-grabs".

Currently only committers can assign issues to themselves so just add a comment if you're starting work on it.

A nice gentle way to contribute would be to review the [standard library docs](https://kotlinlang.org/api/latest/jvm/stdlib/index.html)
and find classes or functions which are not documented very well and submit a patch.

In particular, it'd be great if all functions included a nice example of how to use it such as for the
[`hashMapOf()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/hash-map-of.html) function.
This is implemented using the [`@sample`](https://github.com/JetBrains/kotlin/blob/1.1.0/libraries/stdlib/src/kotlin/collections/Maps.kt#L91)
macro to include code from a test function. The benefits of this approach are twofold; First, the API's documentation is improved via beneficial examples that help new users and second, the code coverage is increased.

Some of the code in the standard library is created by generating code from templates. See the [README](/libraries/stdlib/ReadMe.md) in the stdlib section for how to run the code generator. The existing templates can be used as examples for creating new ones.

Also the [JavaScript back-end](https://github.com/JetBrains/kotlin/blob/master/js/ReadMe.md) could really use your help. See the [JavaScript contribution section](https://github.com/JetBrains/kotlin/blob/master/js/ReadMe.md) for more details.

You can also [contribute to Kotlin/Native](../kotlin-native/README.md).

If you want to contribute a new language feature, it is important to discuss it through a [KEEP](https://github.com/Kotlin/KEEP) first and get an approval from the language designers. This way you'll make sure your work will be in line with the overall language evolution plan and no other design decisions or considerations will block its acceptance.

## Submitting patches

The best way to submit a patch is to [fork the project on GitHub](https://help.github.com/articles/fork-a-repo/) and then send us a
[pull request](https://help.github.com/articles/creating-a-pull-request/) to the `master` branch via [GitHub](https://github.com).

If you create your own fork, it might help to enable rebase by default
when you pull by executing
``` bash
git config --global pull.rebase true
```
This will avoid your local repo having too many merge commits
which will help keep your pull request simple and easy to apply.

## Rules for commit messages

Most of these rules are originated from the [How to Write a Git Commit Message](https://chris.beams.io/posts/git-commit/) 
article, and it's highly recommended to read it.

### Rules on commit messages' content

1. Use the body to explain what and why vs. how
   * Please make an extra effort to explain why changes are needed for every non-trivial modification.
2. Significant commits must mention relevant [YouTrack](https://youtrack.jetbrains.com/issues/KT) issues in their messages
3. Commit changes together with the corresponding tests, unless the resulting commit becomes too unwieldy to grasp
4. Try to avoid commits like *"Fixes after review"* whenever it's possible and squash them with meaningful ones instead
5. Keep the subject (first line of the commit message) clean and readable. All additional information and directives for external tools 
should be moved to the message body.
6. If you mention “*^[KTIJ-235](https://youtrack.jetbrains.com/issue/KTIJ-235) Fixed*” in the message body - then the VCS integration will 
add *Fix in Builds* field to the issue in the YouTrack and automatically mark issue as fixed. For details see 
[YouTrack Integrations](https://www.jetbrains.com/youtrack/features/integrations.html) and 
[Apply Commands in VCS Commits](https://www.jetbrains.com/help/youtrack/server/Apply-Commands-in-VCS-Commits.html).

### Rules on commit messages' style/formatting

1. Separate subject from body with a blank line
2. Capitalize the subject line
3. Do not end the subject line with a period
4. Use the imperative mood in the subject line
5. Limit the commit messages lines to 72 characters
   * Use “Commit Message Inspections” in IntelliJ IDE *Settings -> Version Control -> Commit*
   * vim: ```autocmd FileType gitcommit setlocal textwidth=72```

## Checklist

Before submitting the pull request, make sure that you can say "YES" to each point in this short checklist:

  - You provided the link to the related issue(s) from YouTrack
  - You made a reasonable amount of changes related only to the provided issues
  - You can explain changes made in the pull request
  - You ran the build locally and verified new functionality
  - You ran related tests locally and they passed
  - You do not have merge commits in the pull request
