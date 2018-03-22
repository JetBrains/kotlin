package org.test

@Experimental(Experimental.Level.ERROR, [Experimental.Impact.LINKAGE])
annotation class Experimental1

@Experimental(Experimental.Level.ERROR, [Experimental.Impact.RUNTIME])
annotation class Experimental2
