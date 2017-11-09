// "Add Android View constructors using '@JvmOverloads'" "true"
// ERROR: This type has a constructor, and thus must be initialized here
// WITH_RUNTIME

package com.myapp.activity

import android.view.View

class Foo : View<caret>