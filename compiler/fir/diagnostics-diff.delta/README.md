# Diagnostics diff delta tool

This tool extracts data about the differences in diagnostics tests (e.g., `compiler/testData/diagnostics`) for FE 1.0 versus FIR, at two
different commits, and computes the delta between the two sets of data. The purpose of this tool is to show the impact of a set of changes
(between the two commits) in getting FIR diagnostics closer to FE 1.0 diagnostics. For example, this tool may be used to see whether
your changes got all the diagnostics that you expect to be reported, or if you missed some cases.

## Invoking the tool

To install the tool:
```shell script
./gradlew :compiler:fir:diagnostics-diff.delta:installDist
```

The executable can be found at `compiler/fir/diagnostics-diff.delta/build/install/diagnostics-diff-delta/bin/diagnostics-diff-delta`. Run it
to see the help and available options.

Sample usage:
```shell script
diagnostics-diff-delta --format HTML --repo JetBrains/kotlin --before 787c7433339666fef49a56d63197e422318466d4 --after db55a973d414090d0e815fa4573dd8d6c5b95f35 compiler/testData/diagnostics $HOME/diagnostics-delta-html
```

This will compare the test data at `compiler/testData/diagnostics` and produce an HTML report (the other format is `CSV`) at
`$HOME/diagnostics-delta-html`. It will compare the test data at commit `787c7433339666fef49a56d63197e422318466d4` and then at
`db55a973d414090d0e815fa4573dd8d6c5b95f35`. Those pages will contain links to GitHub so you can view the test data files. The links will
point to the `JetBrains/kotlin` repository on GitHub.

**Note:** You _must_ run the tool from inside a local Git repository as it will check out the "before" and "after" commits. It will restore
the original branch (or commit if HEAD is detached) after it is done. In addition, you should _not_ use `master` or any branch name for the
commits as those branches' heads may change over time. If either "before" or "after" commit is _not_ in a remote GitHub repository, then
omit the `--repo` option completely. The tool will copy the test data files (for both "before" and "after" commits) to the output.

## Interpreting the data

**Note:** Please read `compiler/fir/diagnostics-diff.report/README.md` first as it will explain the terms. 

This tool basically runs the `diagnostics-diff-report` tool at the "before" and "after" commits, then takes the delta between the two
generated reports. The `index.html` page will contain links at the top for the original reports at the "before" and "after" commits.
The CSV or the `index.html` in the delta report is simply the difference between the two original reports. I.e., take the value of each cell
in the "after" report and subtract the corresponding value from the "before" report.

In the HTML report, clicking on any of the links on a row will take you to a page that shows details for that diagnostic code. You can see
what has changed between the two commits. For example, under "Missing (-5)" you may see "Removed (-8)" and "Added (+3)". This means that
with the changes, 8 of the diagnostics are no longer missing, but 3 are now missing. Those 8 could be matched now, or possibly mismatched;
the only way to be sure is to compare the files. Those 3 could be regressions; again, you will have to examine the test.

Clicking on one of the file links will take you to a page that shows the diffs between the test data files for:
1. FIR 1.0 "before" and FIR "after" — Helps answer the question _"How did the changes improve FIR?"_
2. FE 1.0 "after" and FIR "after" — Helps answer the question _"After the changes, how far is FIR from FE 1.0?"_
3. FE 1.0 "before" and FIR "before" — Helps answer the question _"Before the changes, how far was FIR from FE 1.0?"_