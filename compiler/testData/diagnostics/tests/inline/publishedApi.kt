// !DIAGNOSTICS: -EXPOSED_PARAMETER_TYPE -NOTHING_TO_INLINE
inline fun call(a: A) {
    a.test()
    publishedTopLevel()
    
    a.publishedVar
    a.publishedVar = 1

    publishedVarTopLevel
    publishedVarTopLevel = 1
}

inline var inlineVar: Int
    get() {
        val a = A()
        a.test()
        publishedTopLevel()

        a.publishedVar
        a.publishedVar = 1

        publishedVarTopLevel
        publishedVarTopLevel = 1
        return 1
    }
    set(value) {
        val a = A()
        a.test()
        publishedTopLevel()

        a.publishedVar
        a.publishedVar = 1

        publishedVarTopLevel
        publishedVarTopLevel = 1
    }

@PublishedApi
internal class A {
    @PublishedApi
    internal fun test() {
        publicFun()
        internalFun()
        privateFun()

        publicVarTopLevel
        publicVarTopLevel = 1
        internalVarTopLevel
        internalVarTopLevel = 1
        privateVarTopLevel
        privateVarTopLevel = 1

        publishedVar
        publishedVar = 1
    }

    @PublishedApi
    internal inline fun testInline() {
        publicFun()
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalFun<!>()
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateFun<!>()

        publicVarTopLevel
        publicVarTopLevel = 1
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalVarTopLevel<!>
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE, NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalVarTopLevel<!> = 1
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateVarTopLevel<!>
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE, NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateVarTopLevel<!> = 1

        publishedVar
        publishedVar = 1
    }


    @PublishedApi
    internal inline var publishedVar: Int
        get() {
            publicFun()
            <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalFun<!>()
            <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateFun<!>()

            publicVarTopLevel
            publicVarTopLevel = 1
            <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalVarTopLevel<!>
            <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE, NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalVarTopLevel<!> = 1
            <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateVarTopLevel<!>
            <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE, NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateVarTopLevel<!> = 1
            return 1
        }

        set(value) {
            publicFun()
            <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalFun<!>()
            <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateFun<!>()

            publicVarTopLevel
            publicVarTopLevel = 1
            <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalVarTopLevel<!>
            <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE, NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalVarTopLevel<!> = 1
            <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateVarTopLevel<!>
            <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE, NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateVarTopLevel<!> = 1
        }

}

@PublishedApi
internal fun publishedTopLevel() {
    publicFun()
    internalFun()
    privateFun()

    publicVarTopLevel
    publicVarTopLevel = 1
    internalVarTopLevel
    internalVarTopLevel = 1
    privateVarTopLevel
    privateVarTopLevel = 1
}

@PublishedApi
inline internal fun publishedTopLevelInline() {
    publicFun()
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalFun<!>()
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateFun<!>()

    publicVarTopLevel
    publicVarTopLevel = 1
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalVarTopLevel<!>
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE, NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalVarTopLevel<!> = 1
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateVarTopLevel<!>
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE, NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateVarTopLevel<!> = 1
}

@PublishedApi
inline internal var publishedVarTopLevel: Int
    get() {
        publicFun()
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalFun<!>()
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateFun<!>()

        publicVarTopLevel
        publicVarTopLevel = 1
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalVarTopLevel<!>
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE, NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalVarTopLevel<!> = 1
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateVarTopLevel<!>
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE, NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateVarTopLevel<!> = 1
        return 1
    }

    set(value) {
        publicFun()
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalFun<!>()
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateFun<!>()

        publicVarTopLevel
        publicVarTopLevel = 1
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalVarTopLevel<!>
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE, NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>internalVarTopLevel<!> = 1
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateVarTopLevel<!>
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE, NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateVarTopLevel<!> = 1
    }


fun publicFun() {}

internal fun internalFun() {}

private fun privateFun() {}


var publicVarTopLevel = 1

internal var internalVarTopLevel = 1

private var privateVarTopLevel = 1