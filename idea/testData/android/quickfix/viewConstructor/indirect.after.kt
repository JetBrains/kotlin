// "Add Android View constructors using '@JvmOverloads'" "true"
// ERROR: This type has a constructor, and thus must be initialized here
// WITH_RUNTIME

package com.myapp.activity

import android.content.Context
import android.util.AttributeSet
import android.view.TextView

class Foo @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : TextView<caret>(context, attrs, defStyleAttr)