# Diagnostic Message Style Guide

This guide describes how to phrase the messages of compiler diagnostics (errors and warnings).
It applies both to writing new messages and to proofreading existing ones.

## Style guide

* Sentences and fragments should end with a full stop.
* Use single quotes to delimit code, e.g., in parameters.
  * Single quotes need to be escaped with another single quote when the message has parameters.
* Use American English spelling.
* Avoid "we", "us" and "you".
  * Prefer "consider doing ..." to "you can do ...".
  * Prefer "do something" to "you should do something".
* Avoid unnecessary politeness/uncertainty.
  * Avoid "please".
  * Avoid "could", "should", etc.
    * Use "must" over "should" for hard requirements.
  * Avoid using "probably", "likely", "maybe".
* Avoid contractions like "can't", "won't", "don't".
* Avoid the term "useless".
  * Use "redundant" if the problem doesn't affect the compilation result and is purely cosmetic.
  * Describe the problem more precisely (e.g. "unreachable else" instead of "useless else").
* Use "cannot" over "mustn't" or "must not" to express something forbidden or impossible.
* Use <https://kotl.in> short links if you need to add links to diagnostic messages.
  * Exception: YouTrack links are fine.

## Examples

> Lambda expression is unused. If you mean a block, you can use 'run { ... }'.

*Bad:* Suggestion is overly polite, corrective, and doesn't match the reported problem very well.

> Lambda expression is never invoked. To create a scoped block, use 'run { ... }'.

*Better:* Suggestion is direct, instructive, and better matches the reported problem.

## For AI agents

When generating a new diagnostic message or proofreading an existing one:

1. Apply every rule from the [Style guide](#style-guide) section.
2. Diagnostic messages are defined in the `*DefaultMessages*` / `*DefaultErrorMessages*` files, most notably:
   * `compiler/fir/checkers/src/org/jetbrains/kotlin/fir/analysis/diagnostics/FirErrorsDefaultMessages.kt`
   * Platform-specific ones: `FirJvmErrorsDefaultMessages`, `FirJsErrorsDefaultMessages`, `FirWasmErrorsDefaultMessages`,
     `FirNativeErrorsDefaultMessages`, `FirWebCommonErrorsDefaultMessages` (under `compiler/fir/checkers/checkers.*`)
   * Syntax errors: `compiler/fir/raw-fir/raw-fir.common/src/org/jetbrains/kotlin/fir/builder/FirSyntaxErrorsDefaultMessages.kt`
   * Compiler plugins: e.g. `KtDefaultErrorMessagesSerialization`, `KtDefaultErrorMessagesParcelize`
3. If the message has parameters (`{0}`, `{1}`, ...), every literal single quote must be doubled (`''`).
   If it has no parameters, single quotes are written as is.
4. When suggesting a fix, prefer instructive phrasing ("To do 'goal', use 'solution'.")
   over corrective phrasing ("If you are attempting to do 'goal', use 'solution'.").
5. Changing a message affects `.fir.diag.txt` test data (tests with the `RENDER_DIAGNOSTICS_FULL_TEXT` directive).
   Regenerate it with `-Pkotlin.test.update.test.data=true`, see [analysis-tests/AGENTS.md](../compiler/fir/analysis-tests/AGENTS.md).
6. Do not reword unrelated existing messages unless asked: it might cause test data churn across the repository.
   When proofreading, report violations of this guide instead.
