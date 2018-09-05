# Diagnostic specification tests

Diagnostic specification tests are diagnostic tests for certain statements in the [Kotlin language specification](https://github.com/JetBrains/kotlin-spec).

Note: diagnostic tests format specification you can see in the [diagnostic tests readme](https://github.com/JetBrains/kotlin/blob/master/compiler/testData/diagnostics/ReadMe.md).

## Structure

Each test relates to a specific section, paragraph, and sentence of the Kotlin language specification, and is either positive or negative.

The folder structure is as follows:
* `<sectionName>`
    * `p-<paragraphNumber>`
        * `<neg|pos>`
            * `<setenceNumber>.<testNumber>.kt` (test source code)
            * `<setenceNumber>.<testNumber>.txt` (descriptors file)

Example test file path: `testData/diagnostics/linked/when-expression/p-2/neg/3.1.kt`

## Positive and negative tests

Positive tests are considered to be tests in which there is no single diagnostics with a `ERROR` severity.
Positive tests can only contain diagnostics with a `WARNING` or `INFO` severity (or not contain at all).

In a negative test, there must be at least one diagnostic with `ERROR` severity.

## Tests format

### Test description

Each test file must contain meta information in the form of a multi-line comment.

A comment with meta information has the following format:
```
/*
 KOTLIN SPEC TEST (<POSITIVE|NEGATIVE>)

 SECTION: <sectionName>
 PARAGRAPH: <paragraphNumber>
 SENTENCE: [<setenceNumber>] <setence>
 NUMBER: <testNumber>
 DESCRIPTION: <testDescription>
 */
```
Example:
```
/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTION: when-expression
 PARAGRAPH: 2
 SENTENCE: [3] When expression has two different forms: with bound value and without it.
 NUMBER: 1
 DESCRIPTION: Empty 'when' with bound value.
 */
```

Meta-information should be placed at the beginning of the file after directives (if any).

### Case description

The test can contain many cases.
Each case (if there are more than one) should be accompanied by a description.

The case description is a single-line comment of the following format:
```
// CASE DESCRIPTION: <caseDescription>
```
Example:
```
// CASE DESCRIPTION: Checking for exhaustive 'when' (all sealed class subtypes and null value are covered).
fun case_1(expr: Expr?): String = when (expr) {
    is Const -> <!DEBUG_INFO_SMARTCAST!>expr<!>.n
    is Sum -> <!DEBUG_INFO_SMARTCAST!>expr<!>.e1 + <!DEBUG_INFO_SMARTCAST!>expr<!>.e2
    is Mul -> <!DEBUG_INFO_SMARTCAST!>expr<!>.m1 + <!DEBUG_INFO_SMARTCAST!>expr<!>.m2
    null -> ""
}
```

## Test validation

Before running the test, the following validation is performed:
- check for correspondence to the format of the folders and file names;
- check for correspondence to the format of the meta-information in the test file;
- check for consistency between the location and name of the test file and the meta-information in it;
- checking whether the test is positive or negative using information about diagnostics severity.

If the validation fails, you will receive exception about it.

## Test run log

During the test run, the following information is displayed for each test:

```
DIAGNOSTICS <POSITIVE|NEGATIVE> SPEC TEST
SECTION: <sectionName> (paragraph: <paragraphNumber>)
SENTENCE <sentenceNumber>: <sentence>
TEST NUMBER: <testNumber>
TEST CASES: <casesNumber>
DESCRIPTION: <testDescription>
DIAGNOSTICS: <diagnosticSeverities> | <diagnostics>
```
Example:
```
DIAGNOSTICS NEGATIVE SPEC TEST
SECTION: when-expression (paragraph: 3)
SENTENCE 1: 
TEST NUMBER: 1
NUMBER OF TEST CASES: 3
DESCRIPTION: 'When' without bound value and not allowed break and continue expression (without labels) in the control structure body.
DIAGNOSTICS: {ERROR=2, WARNING=1} | {BREAK_OR_CONTINUE_IN_WHEN=2, UNREACHABLE_CODE=1}
```

## Statistics on specification tests

To see statistics for existing tests you can run gradle task `printSpecTestStatistic` in the `:compiler:tests-spec`.

Example output:
```

==================================================
SPEC TESTS STATISTIC
--------------------------------------------------
PSI: 0 tests
DIAGNOSTICS: 54 tests
  when-expression: 54 tests
    PARAGRAPH 9: 8 tests [ neg: 4 ] [ pos: 4 ]
    PARAGRAPH 7: 16 tests [ neg: 7 ] [ pos: 9 ]
    PARAGRAPH 6: 2 tests [ neg: 1 ] [ pos: 1 ]
    PARAGRAPH 11: 17 tests [ neg: 8 ] [ pos: 9 ]
    PARAGRAPH 3: 7 tests [ neg: 3 ] [ pos: 4 ]
    PARAGRAPH 2: 2 tests [ pos: 2 ]
    PARAGRAPH 5: 2 tests [ neg: 1 ] [ pos: 1 ]
CODEGEN: 0 tests

```