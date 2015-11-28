package test

inline fun <reified T, reified R>T.castTo(): R = this as R
