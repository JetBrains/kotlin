## Inference

Currently, this document contains some basic terms that are common for different specific inference types.
Lately, it might be extended to include some basic description of how inference works.

### Glossary
#### CS = Constraint system
An instance of `org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl`
#### Call-tree
A tree of calls, in which constraint systems are joined and solved(completed) together
#### Proper constraint
A constraint that doesn't reference any type variables