# Contributing

We love contributions! There's [lots to do on Kotlin](https://youtrack.jetbrains.com/issues/KT) and on the
[standard library](https://youtrack.jetbrains.com/issues/KT?q=%23Kotlin%20%23Unresolved%20and%20(links:%20KT-2554,%20KT-4089%20or%20%23Libraries)) so why not chat with us
about what you're interested in doing? Please join the #kontributors channel in [our Slack chat](http://slack.kotlinlang.org/)
and let us know about your plans.

If you want to find some issues to start off with, try [this query](https://youtrack.jetbrains.com/issues/KT?q=tag:%20%7BUp%20For%20Grabs%7D%20and%20State:%20Open) which should find all open Kotlin issues that are marked as "up-for-grabs".

Currently only committers can assign issues to themselves so just add a comment if you're starting work on it.

A nice gentle way to contribute would be to review the [standard library docs](https://kotlinlang.org/api/latest/jvm/stdlib/index.html)
and find classes or functions which are not documented very well and submit a patch.

In particular it'd be great if all functions included a nice example of how to use it such as for the
[`hashMapOf()`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/hash-map-of.html) function.
This is implemented using the [`@sample`](https://github.com/JetBrains/kotlin/blob/1.1.0/libraries/stdlib/src/kotlin/collections/Maps.kt#L91)
macro to include code from a test function. The benefits of this approach are twofold; First, the API's documentation is improved via beneficial examples that help new users and second, the code coverage is increased.

Some of the code in the standard library is created by generating code from templates. See the [README](libraries/stdlib/ReadMe.md) in the stdlib section for how to run the code generator. The existing templates can be used as examples for creating new ones.

Also the [JavaScript back-end](https://github.com/JetBrains/kotlin/blob/master/js/ReadMe.md) could really use your help. See the [JavaScript contribution section](https://github.com/JetBrains/kotlin/blob/master/js/ReadMe.md) for more details.

If you want to contribute a new language feature, it is important to discuss it through a [KEEP](https://github.com/Kotlin/KEEP) first and get an approval from the language designers. This way you'll make sure your work will be in line with the overall language evolution plan and no other design decisions or considerations will block its acceptance.

## Submitting patches

The best way to submit a patch is to [fork the project on GitHub](https://help.github.com/articles/fork-a-repo/) and then send us a
[pull request](https://help.github.com/articles/creating-a-pull-request/) via [GitHub](https://github.com).

If you create your own fork, it might help to enable rebase by default
when you pull by executing
``` bash
git config --global pull.rebase true
```
This will avoid your local repo having too many merge commits
which will help keep your pull request simple and easy to apply.

## Checklist

Before submitting the pull request, make sure that you can say "YES" to each point in this short checklist:

  - You provided the link to the related issue(s) from YouTrack
  - You made a reasonable amount of changes related only to the provided issues
  - You can explain changes made in the pull request
  - You ran the build locally and verified new functionality
  - You ran related tests locally and they passed
  - You do not have merge commits in the pull request
