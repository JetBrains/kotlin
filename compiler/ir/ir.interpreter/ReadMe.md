This module enables interpretation of IR, which is used for evaluating `const` variables and functions marked with `@IntrinsicConstEvaluation` at compile time, as an optimization.

##### Note:
There is also a separate code interpretation mechanism over FIR.
It is used for activities that cannot be done at IR level, such as reporting diagnostics (e.g. `n / (1 - 1)` reports _Division by zero_ warning).