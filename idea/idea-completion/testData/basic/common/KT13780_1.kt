class ResolvedCall<out D>(val candidateDescriptor: D)

fun test(myResolvedCall: ResolvedCall<Any>) {
    bar(my<caret>)
}

fun bar(foo: ResolvedCall<out CharSequence>) {}

// EXIST: myResolvedCall