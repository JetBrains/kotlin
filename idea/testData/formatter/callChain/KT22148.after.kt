fun foo() {
    async {
        // ...
    }.logFailures().invokeOnCompletion {
        if (it != null) showErrorAndContinue()
        finish()
    }

    FirebaseAuth.getInstance().addAuthStateListener(
            object : FirebaseAuth.AuthStateListener {
                override fun onAuthStateChanged(auth: FirebaseAuth) {
                    // ...
                }
            },
    )
}
