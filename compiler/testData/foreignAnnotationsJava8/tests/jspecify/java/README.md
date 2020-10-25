# Sample inputs

## Disclaimers

**These sample inputs are an experiment.**

They use annotations whose names and meanings are not finalized.

They have not been code reviewed.

We do not know if this is even the format that we want our sample inputs to be
in.

Whatever our final samples look like, we do **not** expect to present them as
"conformance tests" that require any behavior from tools: The goal of our
project is to provide one source of nullness information. Tools may use some,
all, or none of that information. They may also use information from other
sources. Based on the information they have available for any given piece of
code, tools always have the option to issue a warning / error / other diagnostic
for that code, and they always have the option not to.

The hope is that samples like these may be useful to those who wish to discuss
the spec or implement tools. To that end, we welcome comments on both the format
of these files and their content.

## Directory structure

See
[jspecify: test-data format: Directory structure](https://docs.google.com/document/d/1JVH2p61kReO8bW4AKnbkpybPYlUulVmyNrR1WRIEE_k/edit#bookmark=id.2t1r58i5a03s).
TODO(#134): Inline that here if Tagir can sign the CLA and contribute it.

Additionally:

Fully qualified class names must be unique across all directories.

> This permits all files to be compiled in a single tool invocation.

TODO: Consider requiring that all individual-file samples be in the top-level
directory.

Each file must contain a single top-level class. TODO(#133): Consider relaxing
this.

TODO: Consider requiring a file's path to match its package and class:

-   individual-file samples: `Foo.java` for `Foo`

-   full-directory samples: `sampleFoo/Foo.java` for `Foo`,
    `sampleFoo/bar/Foo.java` for `bar.Foo`

-   We may need additional accommodations for JPMS support to demonstrate
    module-level defaulting.

## Restrictions

Files must be UTF-8 encoded.

Files must contain only printable ASCII characters and `\n`.

Files must be compatible with Java 8. TODO(#131): Decide how to label files that
require a higher version so that we can allow them. (But still encourage
sticking to Java 8 except for tests that specifically exercise newer features.)

Files must compile without error using stock javac.

Files must not depend on any classes other than the JSpecify annotations. This
includes the Java platform APIs. Exception: Files may use `java.lang.Object`,
but they still must not use its methods.

> For example, files may use `Object` as a bound, parameter, or return type.

Files should avoid depending on the presence of absence of "smart" checker
features, such as:

-   looking inside the body of a method to determine what parameters it
    dereferences or what it returns

    -   To that end, prefer abstract methods when practical.

-   flow-sensitive typing

We also encourage writing files that demonstrate individual behaviors in
isolation. For example, we encourage writing files to minimize how much they
rely on type inference -- except, of course, for any files explicitly intended
to demonstrate type inference.

## What sample inputs demonstrate

Sample inputs demonstrate 2 cases:

1.  JSpecify annotations are applied in a way that is
    [structurally illegal](https://docs.google.com/document/d/15NND5nBxMkZ-Us6wz3Pfbt4ODIaWaJ6JDs4w6h9kUaY/edit#heading=h.ib00ltjpj1xa).

    <!-- TODO: Are we happy with the term "illegal?" -->

2.  The second case is more nuanced: Based on JSpecify annotations and rules,
    the code's types can all be augmented with nullness information. We could
    then apply normal JLS rules to those types. (For example, the value of a
    `return` statement must be convertible to the method's return type.) We
    could adapt those rules to use JSpecify rules for subtyping. Based on that,
    we could identify type checks that would fail. (For example, `return null`
    in a method that must return a non-nullable type.)

    <!-- TODO: Update links to point to the markup-format spec and glossary. -->

## Syntax

We define a format for Java comments to identify code that demonstrates the
cases above.

A comment on a given line provides information about the following line.

Such a comment contains one of 5 special sequences. The first 3 cover case 1
from above:

-   `jspecify_conflicting_annotations`: for cases like `@Nullable
    @NullnessUnspecified Foo`

-   `jspecify_unrecognized_location`: for cases like `class @Nullable Foo {}`,
    in which JSpecify does not currently specify meaning for annotations on a
    given location but we can imagine uses for them

-   `jspecify_nullness_intrinsically_not_nullable`: for cases like `@Nullable
    int`

The remaining 2 cover case 2:

-   `jspecify_nullness_mismatch`: for certain instances of case 2

-   `jspecify_nullness_not_enough_information`: for certain instances of case 2

The difference between the "mismatch" and "not enough information" cases arises
from
[unspecified nullness](https://docs.google.com/document/d/1KQrBxwaVIPIac_6SCf--w-vZBeHkTvtaqPSU_icIccc/edit#bookmark=id.xb9w6p3ilsq3).
The rough idea is that a type with unspecified nullness means: "When this code
is annotated for nullness, either the type should have `@Nullable` on it, or it
shouldn't."

-   In a "mismatch," the type check described above would fail no matter how the
    unspecified types are eventually annotated. (This is trivially the case
    whenever a failing type check involves no unspecified types.)

-   In a "not enough information," there is at least one way that the
    unspecified types could be annotated that would _fail_ the type checks, and
    there is at least one way that the unspecified types could be annotated that
    would _pass_ the type checks.

Another way to look at it is how tools are likely (but not obligated!) to
behave:

-   For "mismatch," all tools are likely to report an error. (This assumes that
    a tool implements a given type check: For example, if a tool doesn't check
    type arguments at all, then naturally it wouldn't report errors for them)

-   For "not enough information," tools might not report anything, or they might
    report a warning, or they might report an error. We've sometimes spoken of
    these different behaviors as the difference between a "strict mode" and a
    "lenient mode."

TODO: Consider additional features:

-   multiline comments
-   other locations for comments
-   multiple findings per line/comment
-   comments that apply to larger ranges -- possibly to syntax elements (like
    statements) rather than lines
-   comments that apply only to a particular part of the line

## More TODOs

TODO: Consider how to map between samples and related GitHub issues (comments,
filenames?).
