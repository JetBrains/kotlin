# code-rules.md format

Applies to: `code-rules.md`

A rule must define file patterns with an `Applies to:` directive right after the rule title (`# $name`).
Empty lines are allowed between the title and the directive.
Patterns go into a code block right after the directive, one pattern per line.
A single pattern can use the same syntax, or, alternatively, go on the same line as the directive,
in backticks instead of a code block.

A rule must have exactly one `Applies to:` directive. The directive is case- and whitespace-sensitive.

# Wire task outputs to inputs instead of using dependsOn

Applies to:
```
*.gradle.kts
*.gradle
repo/gradle-build-conventions/
repo/gradle-settings-conventions/
repo/kotlin-build-helpers/
libraries/tools/kotlin-gradle-plugin/src/
!**/src/test/resources/**
```

Don't add `dependsOn` for task dependencies that Gradle can infer: use the output provider of the producer task as
the input of the consumer (e.g. `producerTask.flatMap { it.outputFile }`,
`from(jarTask.map { zipTree(it.archiveFile) })`) instead of `dependsOn` combined with an independently computed path.
This also applies to a `dependsOn` on a provider that already reaches the task through one of its inputs.
Don't report code without an explicit `dependsOn` or `builtBy`.
