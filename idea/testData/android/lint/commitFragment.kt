// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintCommitTransactionInspection

import android.app.Activity
import android.app.FragmentTransaction
import android.os.Bundle

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //OK
        val transaction = fragmentManager.beginTransaction()
        val transaction2: FragmentTransaction
        transaction2 = fragmentManager.beginTransaction()
        transaction.commit()
        transaction2.commit()

        //WARNING
        @Suppress("UNUSED_VARIABLE")
        val transaction3 = fragmentManager.<warning>beginTransaction</warning>()

        //OK
        fragmentManager.beginTransaction().commit()
        fragmentManager.beginTransaction().add(null, "A").commit()

        //OK KT-14470
        Runnable {
            val a = fragmentManager.beginTransaction()
            a.commit()
        }
    }
}
