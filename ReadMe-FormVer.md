# Formal Verification

## Git Usage

In order to permit changes to eventually be merged upstream,
we use a rebase-oriented system.  The `master` branch of
this repository is kept in sync with [JetBrains/kotlin][0],
and `formal-verification` is the defacto master branch for
our work.

To work in this style, ensure that `pull.rebase` is set to
`true`.  Then:

1. To get upstream changes, sync `master` with upstream
   (this can be done on GitHub) and then run `git rebase master formal-verification`.
2. To move changes to a personal branch, do `git rebase formal-verification my-work-branch`.
3. To move changes from a personal branch, make a pull request
   via GitHub to `formal-verification`.  This should be a
   fast forward.

Upstream changes may need to be merged multiple times, as
merging them into `formal-verification` isn't enough to make
them compatible with other branches.  It's not clear what we
can do to improve this.

[0]: https://github.com/JetBrains/kotlin
