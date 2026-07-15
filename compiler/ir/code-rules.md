# Don't lose IR nodes

Pattern: src

When transforming Kotlin backend IR, make sure to preserve all expression nodes,
unless it is known that the node can't have side effects.
