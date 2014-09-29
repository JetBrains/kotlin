// The type checker used to think that T is not null no matter what the upper bound

fun nullableUpperBound<T, INDIRECT: T>(t: T, ind: INDIRECT) {
    if (t == null) {} // was a warning
    if (t != null) {}  // was a warning
    if (ind == null) {}  // was a warning
    if (ind != null) {}  // was a warning
}

fun notNullUpperBound<T: Any, INDIRECT: T>(t: T, ind: INDIRECT) {
    if (<!SENSELESS_COMPARISON!>t == null<!>) {} // still a warning
    if (<!SENSELESS_COMPARISON!>t != null<!>) {} // still a warning
    if (<!SENSELESS_COMPARISON!>ind == null<!>) {} // still a warning
    if (<!SENSELESS_COMPARISON!>ind != null<!>) {} // still a warning
}

