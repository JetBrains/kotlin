package c

import b.B

// There should be _no_ error despite the fact that B and B#foo are annotated with an annotation which cannot be resolved
fun bar(b: B) {
    b.foo("")
}
