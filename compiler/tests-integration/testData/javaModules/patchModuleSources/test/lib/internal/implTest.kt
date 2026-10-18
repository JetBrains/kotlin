package lib.internal

import lib.api.Api

fun test(): String = Api.api() + Impl.impl() + Impl.packagePrivate() + JavaHelper.helper()
