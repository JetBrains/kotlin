# Diagnostics diff tool

This tool extracts data about the differences in diagnostics tests (e.g., `compiler/testData/diagnostics`) for FE 1.0 versus FIR. The two
frontends generate different diagnostics when there is a corresponding `.fir.kt` test data file. The goal is not to get a 100% match, but
the diff can show gaps in the implementation of FIR diagnostics.

## Invoking the tool

To install the tool:
```shell script
./gradlew :compiler:fir:diagnostics-diff.report:installDist
```

The executable can be found at `compiler/fir/diagnostics-diff.report/build/install/diagnostics-diff-report/bin/diagnostics-diff-report`. Run
it to see the help and available options. You can also directly invoke the tool from Gradle:
```shell script
./gradlew :compiler:fir:diagnostics-diff.report:run --args="<options>"
```

Sample usage:
```shell script
diagnostics-diff-report --format HTML --repoAndCommit JetBrains/kotlin/0c7756510497ffcb5bf4bde36043b5bdb919423f compiler/testData/diagnostics $HOME/diagnostics-diff-html
```

This will compare the test data at `compiler/testData/diagnostics` and produce an HTML report (the other format is `CSV`) at
`$HOME/diagnostics-diff-html`. The report will have pages that compare the FE 1.0 and FIR versions of a test data file. Those pages will
contain links to GitHub so you can view the entire FE 1.0 or FIR test data file. The links will point to the `JetBrains/kotlin` repository
on GitHub at commit `0c7756510497ffcb5bf4bde36043b5bdb919423f`.

**Note:** You should use the GitHub repository and commit you are currently at in your local branch. You should _not_ use `master` for the
commit as that branch's head will change over time. If the given commit is _not_ in a remote GitHub repository, then omit the
`--repoAndCommit` option completely. The tool will copy the test data files to the output.

## Interpreting the data

The CSV or the `index.html` page will contain a table of data with one row per diagnostic code. For example:

|Diagnostic|Total|Match %|# Matched|# Non-matched|# Missing|# Unexpected|# Mismatched|# of Files
|---|---|---|---|---|---|---|---|---
|\<OVERALL>|21047|31.75%|6682|14365|10626|2047|1692|5803
|TYPE_MISMATCH|1608|5.41%|87|1521|845|0|676|470
|UNRESOLVED_REFERENCE|2148|52.47%|1127|1021|391|615|15|661
|...

Explanation of the columns (not in same order as above, sorry):
- **Diagnostic**: The diagnostic code for that row of data. The `<OVERALL>` row is a total for all diagnostics.
- **Missing**: Number of times a diagnostic in FE 1.0 is _not_ found at the exact range of code in FIR.
- **Unexpected**: Number of times a diagnostic in FIR is _not_ found at the exact range of code in FE 1.0.
- **Mismatched**: Number of times a diagnostic in FE 1.0 also has a diagnostic in FIR at the exact range of code, but the diagnostic
code in FIR is different. An example of a mismatch is the same code has `UNSAFE_CALL` in FE 1.0 but `INAPPLICABLE_CANDIDATE` in FIR. In that
example, it will be reported as a "mismatch" for _both_ `UNSAFE_CALL` and `INAPPLICABLE_CANDIDATE`.
- **Matched**: Number of times a diagnostic in FE 1.0 appears at the exact range of code in FIR. The start and end of the range must match
exactly. If either start and/or end is off (e.g., `<!UNRESOLVED_REFERENCE!>some<!>.code` in FE 1.0 versus
`<!UNRESOLVED_REFERENCE!>some.code<!>` in FIR), it will be counted as _both_ "missing" and "unexpected".
- **Total**: Sum of `matched + missing + unexpected + mismatched`. This does _not_ equal the number of times the diagnostic appears across
FE 1.0 and FIR files! A "match" is only counted once for `matched` even though it appears in both FE 1.0 and FIR files.
- **Match %**: Simply `matched / total`.
- **Non-matched**: Sum of `missing + unexpected + mismatched`. The table is sorted by this column in descending order.
- **Files**: Number of test data files the diagnostic appears in. `test.kt` and `test.fir.kt` are considered the same file (`test.kt`) for
this count.

Other notes about the data:
- FE 1.0 files marked with `// FIR_IDENTICAL` (meaning 100% match for that file) are included in the results. Every diagnostic in these
files are counted as "matched".

In the HTML report, clicking on any of the links on a row will take you to a page that shows details for that diagnostic code. You can see
which files contain the missing/unexpected/mismatched/matched diagnostics (with counts). Clicking on one of the file links will take you to
a page that compares the FE 1.0 and FIR test data files.