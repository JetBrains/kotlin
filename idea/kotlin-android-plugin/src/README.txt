** How to build android-common.jar **
 - Get android-common.jar from last android-studio build
 - Run proguard with following config

  -dontskipnonpubliclibraryclassmembers
  -dontoptimize
  -dontobfuscate
  -dontusemixedcaseclassnames
  -dontpreverify
  -verbose
  -dontwarn com.google.common.**,net.n3.**,org.jetbrains.annotations.**,com.intellij.**,org.jetbrains.android.**,com.android.annotations**,com.android.utils**


  -keep,includedescriptorclasses class com.android.tools.idea.gradle.output.parser.PatternAwareOutputParser {
      public <methods>;
  }

  -keep,includedescriptorclasses class com.android.tools.idea.gradle.output.GradleMessage {
      public <methods>;
  }

  -keep class com.android.tools.idea.gradle.output.parser.OutputLineReader {
      public <methods>;
  }

 - Check that module kotlin-android-plugin still compile

** Why this jar is needed **
This is a temporary workaround for IDEA 13, because android-plugin isn't packed into ideaSDK


