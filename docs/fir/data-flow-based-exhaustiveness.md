# Data-Flow-Based Exhaustiveness Checking

Data-flow-based exhaustiveness checking is the idea that our knowledge about
the `when` subject's type and value should be stored and processed by the same
infrastructure that powers our smartcasts.
This allows us to leverage arbitrary smartcast information inferred for the
`when` subject expression even before we begin the analysis of the `when` itself.

DFA stores the available information in structures called `TypeStatement`s,
which, in most cases, are of the form `v: T1 & ... & Tn`.

Since - for the purposes of `when` subject analysis - we also need to track
negative type and value information, the general form of a `TypeStatement` now
becomes: `v: P1 & ... & Pn & ¬(N1 | ... | Nn | typeOf(v1) | ... | typeOf(vn))`,
where `typeOf(a)` stands for "the literal type of `a`" - a structural type whose
only instance is exactly `a`.
The positive types are referred to as "upper bounds" and the negative types as
"lower bounds."

## Merging Type Statements
### Upper Bounds

Depending on the control flow, type statements may need to be merged by either
intersecting or uniting their information together.

In the simple cases without negative information, this is performed as:

* `v: P1 & P2` and `v: P3 & P4`
  * == `v: (P1 & P2) & (P3 & P4)`
  * == `v: P1 & P2 & P3 & P4`
    * Dump everything together into a single cauldron, it trivially satisfies
      our definition of a type statement.


* `v: P1 & P2` or `v: P3 & P4`
  * == `v: (P1 & P2) | (P3 & P4)`
  * ~> `v: CST(P1 & P2, P3 & P4)`
    * Both `P1 & P2` and `P3 & P4` can be represented directly in the compiler
      as `ConeIntersectionType`s, so no additional work is needed.

Note that even though the `CST` call may lose some information
(since we don't have proper union types), it's not an issue because
it's just "moving" the upper bound of what we know a further up.
There's no risk of accidentally inferring anything unsound.

### Lower Bounds

In the case of negative type information, things become a bit trickier:

* `v: P1 & ¬N1` and `v: P2 & ¬N2`
  * == `v: (P1 & ¬N1) & (P2 & ¬N2)`
  * == `v: P1 & ¬N1 & P2 & ¬N2`
  * == `v: P1 & P2 & ¬N1 & ¬N2`
  * == `v: P1 & P2 & ¬(N1 | N2)`
    * This case is nice and straightforward, as this statement trivially satisfies
      our extended definition of a type statement. We can dump everything together just
      like we would previously.


* `v: P1 & ¬N1` or `v: P2 & ¬N2`
  * == `v: (P1 & ¬N1) | (P2 & ¬N2)`
    * There's no direct `ConeKotlinType` representation for a negative type, so
      there's no builtin reprenentation for the intersections `P1 & ¬N1` and
      `P2 & ¬N2` either. Consequently, we are unable to call `CST` since we
      technically have no arguments to supply it with. Therefore, we must properly
      distribute the parentheses.
  * == `v: (P1 | P2) & (P1 | ¬N2) & (P2 | ¬N1) & (¬N1 | ¬N2)`
  * == `v: (P1 | P2) & (P1 | ¬N2) & (P2 | ¬N1) & ¬(N1 & N2)`
    * Now consider the two middle terms: semantically, they represent the fact that
      whenever `v` is _not_ `P1`, it must necessarily be `¬N2`, and whenever it's
      _not_ `P2`, it must necessarily be `¬N1`. These conditions tie together
      _which_ pieces of information are available simultaneously: if we're uniting
     data flow information from parallel CFG paths, these conditions encode which
     pieces of knowledge come from the same one.
    * We allow ourselves to approximate data flow information up and
      ignore these terms, resulting in...
  * ~> `v: (P1 | P2) & ¬(N1 & N2)`
    * As a counter-example, you can consider a `v` which is known to be `P1 & ¬N2`:
      such a variable conforms to this statement, but it doesn't satisfy the
      previous one as it violates the `(P2 | ¬N1)` term. It is actually unobtainable, 
      but we no longer know that.
  * ~> `v: CST(P1, P2) & ¬(N1 & N2)`
    * The second approximation takes place when we recall we neither have union types,
      and must rely on `CST`. Again, this isn't really an issue as we're only moving
      the upper bound further up.

### Complex Lower Bounds

So far so good. But now consider a more complex case where each input type statement
involves multiple negative bounds already. For positives, it's not really an issue:
the existence of `ConeIntersectionType` allows us to go back and forth between
`v: P1 & ... & Pn` and `v: it(P1, ..., Pn)`, and we've already seen how it plays
out in the first subsection of this document. For negatives, things get more complicated.

* `v: ¬(N1 | N2)` and `v: ¬(N3 | N4)`
  * == `v: ¬(N1 | N2) & ¬(N3 | N4)`
    * **Gotcha #1:** running `CST(N1, N2)` or `CST(N3, N4)` would, actually, be incorrect.
      `CST` approximates the result up, but running it under `¬` would mean we're trying
      to bring the lower bound higher, potentially leading to an unsound result.
    * Consider, for example, `v: ¬(Int | Double)`: "approximating" it to `v: ¬Number` is,
      in fact, wrong, as `¬Number` guarantees `v` is not a `Float`, for instance, but
      in reality, it could very well be one.
    * Instead, we should properly distribute the parentheses.
  * == `v: (¬N1 & ¬N2) & (¬N3 & ¬N4)`
  * == `v: ¬N1 & ¬N2 & ¬N3 & ¬N4`
  * == `v: ¬(N1 | N2 | N3 | N4)`
    * Nice, straightforward, discreet. This result conforms to the form of our
      type statement and doesn't require running anything non-trivial.


* `v: ¬(N1 | N2)` or `v: ¬(N3 | N4)`
  * == `v: ¬(N1 | N2) | ¬(N3 | N4)`
  * == `v: (¬N1 & ¬N2) | (¬N3 & ¬N4)`
  * == `v: (¬N1 | ¬N3) & (¬N1 | ¬N4) & (¬N2 | ¬N3) & (¬N2 | ¬N4)`
  * == `v: ¬(N1 & N3) & ¬(N1 & N4) & ¬(N2 & N3) & ¬(N2 & N4)`
  * == `v: ¬(N1 & N3 | N1 & N4 | N2 & N3 | N2 & N4)`
    * **Gotcha #2:** even though we technically can accurately calculate the final
      type for this expression, such an endeavor would result in bringing an
      exponential algorithm into the compiler, which we don't really want to do.
    * So what we do instead is intersect all the lower bounds together in one go
      without building the individual permutations. This way we only end up supporting
      the trivial cases like `v: ¬Number` or `v: ¬Int` => `v: ¬Int`, but fail at more
      complex examples like the one in `complementarySealedVariantsLimitations.kt`.
    * Hypothetically, we could bring support for more edge cases, such as manually
      preserving negative components common to both the statements, but it's too
      hacky and lacks proper motivation.
