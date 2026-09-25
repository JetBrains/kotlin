# code-rules.md format

Applies to: `code-rules.md`

A rule must define file patterns with an `Applies to:` directive right after the rule title (`# $name`).
Empty lines are allowed between the title and the directive.
Patterns go into a code block right after the directive, one pattern per line.
A single pattern can use the same syntax, or, alternatively, go on the same line as the directive,
in backticks instead of a code block.

A rule must have exactly one `Applies to:` directive. The directive is case- and whitespace-sensitive.
