# Diagnostic specification tests

Diagnostic specification tests are diagnostic tests for certain statements in the [Kotlin language specification](https://github.com/JetBrains/kotlin-spec).

Note: diagnostic tests format specification you can see in the [diagnostic tests readme](https://github.com/JetBrains/kotlin/blob/master/compiler/testData/diagnostics/ReadMe.md).

## Structure

Each test relates to a specific section, paragraph, and sentence of the Kotlin language specification, and is either positive or negative.

The folder structure is as follows:
* `s-<sectionNumber>_<sectionName>`
    * `p-<paragraphNumber>`
        * `<neg|pos>`
            * `<setenceNumber>.<testNumber>.kt` (test source code)
            * `<setenceNumber>.<testNumber>.txt` (descriptors file)

Example test file path: `testsSpec/s-16.30_when-expression/p-2/neg/3.1.kt`

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

 SECTION <sectionNumber>: <sectionName>
 PARAGRAPH: <paragraphNumber>
 SENTENCE <setenceNumber>: <setence>
 NUMBER: <testNumber>
 DESCRIPTION: <testDescription>
 */
```
Example:
```
/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 2: Each entry consists of a boolean condition (or a special else condition), each of which is checked and evaluated in order of appearance.
 NUMBER: 1
 DESCRIPTION: 'When' with not boolean condition in 'when condition'
 */
```

Meta-information should be placed at the beginning of the file after diagnostic directives (if any).

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
<POSITIVE|NEGATIVE> DIAGNOSTICS SPEC TEST
SECTION: <sectionNumber> <sectionName> (paragraph: <paragraphNumber>)
SENTENCE <sentenceNumber> [<specUrl>]: <sentence>
TEST NUMBER: <testNumber>
NUMBER OF TEST CASES: <casesNumber>
DESCRIPTION: <testDescription>
DIAGNOSTICS: <diagnosticSeverities> | <diagnostics>
```
Example:
```
POSITIVE DIAGNOSTICS SPEC TEST
SECTION: 16.30 When expression (paragraph: 5)
SENTENCE 1 [http://jetbrains.github.io/kotlin-spec/#when-expression:5:1]: The type of the resulting expression is the least upper bound of the types of all the entries.
TEST NUMBER: 4
NUMBER OF TEST CASES: 9
DESCRIPTION: 'When' least upper bound of the types check (when exhaustive via sealed class).
DIAGNOSTICS: {WARNING=15} | {USELESS_IS_CHECK=1, IMPLICIT_CAST_TO_ANY=14}
```

## Statistics on specification tests

To see statistics for existing tests you can run gradle task `printSpecTestStatistic` in the `:compiler:tests-spec`.

Example output:
```

--------------------------------------------------
SPEC TESTS STATISTIC
--------------------------------------------------
DIAGNOSTICS: 131 tests
  16.30 WHEN-EXPRESSION: 131 tests
    PARAGRAPH 2: 4 tests (neg: 2, pos: 2)
    PARAGRAPH 3: 35 tests (neg: 5, pos: 30)
    PARAGRAPH 4: 67 tests (neg: 11, pos: 56)
    PARAGRAPH 5: 8 tests (neg: 4, pos: 4)
    PARAGRAPH 6: 17 tests (neg: 8, pos: 9)
PSI: 0 tests
CODEGEN: 0 tests
--------------------------------------------------

```