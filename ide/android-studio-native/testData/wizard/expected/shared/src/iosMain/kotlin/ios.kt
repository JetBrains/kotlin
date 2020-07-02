package org.jetbrains.shared

import platform.UIKit.UIDevice

actual class Platform actual constructor() {
    actual val platform = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}
