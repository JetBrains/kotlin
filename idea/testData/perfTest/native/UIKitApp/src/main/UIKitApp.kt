/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")
package perfTestPackage1 // this package is mandatory

import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.ObjCOutlet
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toCValues
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSCoder
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSStringFromClass
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDelegateProtocol
import platform.UIKit.UIApplicationDelegateProtocolMeta
import platform.UIKit.UIApplicationMain
import platform.UIKit.UIButton
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIControlStateNormal
import platform.UIKit.UIFont
import platform.UIKit.UILabel
import platform.UIKit.UIResponder
import platform.UIKit.UIResponderMeta
import platform.UIKit.UIScreen
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.addSubview
import platform.UIKit.backgroundColor
import platform.UIKit.font
import platform.UIKit.heightAnchor
import platform.UIKit.leadingAnchor
import platform.UIKit.setFrame
import platform.UIKit.topAnchor
import platform.UIKit.translatesAutoresizingMaskIntoConstraints
import platform.UIKit.widthAnchor

fun main(args: Array<String>) {
    memScoped {
        val argc = args.size + 1
        val argv = (arrayOf("konan") + args).map { it.cstr.ptr }.toCValues()

        autoreleasepool {
            UIApplicationMain(argc, argv, null, NSStringFromClass(AppDelegate))
        }
    }
}

private class AppDelegate : UIResponder, UIApplicationDelegateProtocol {
    companion object : UIResponderMeta(), UIApplicationDelegateProtocolMeta {}

    @OverrideInit
    constructor() : super()

    private var _window: UIWindow? = null
    override fun window() = _window
    override fun setWindow(window: UIWindow?) {
        _window = window
    }

    override fun application(application: UIApplication, didFinishLaunchingWithOptions: Map<Any?, *>?): Boolean {
        window = UIWindow(frame = UIScreen.mainScreen.bounds)
        window!!.rootViewController = ViewController()
        window!!.makeKeyAndVisible()
        return true
    }
}

@ExportObjCClass
private class ViewController : UIViewController {

    @OverrideInit
    constructor() : super(nibName = null, bundle = null)

    @OverrideInit
    constructor(coder: NSCoder) : super(coder)

    @ObjCOutlet
    lateinit var label: UILabel

    @ObjCOutlet
    lateinit var button: UIButton

    var pressed = 0

    @ObjCAction
    fun buttonPressed() {
        label.text = "Hello #${pressed++} from Konan!"
        println("Button pressed")
    }

    override fun viewDidLoad() {
        super.viewDidLoad()

        val (width, height) = UIScreen.mainScreen.bounds.useContents {
            this.size.width to this.size.height
        }

        val header = UIView().apply {
            backgroundColor = UIColor.lightGrayColor
            view.addSubview(this)
            translatesAutoresizingMaskIntoConstraints = false
            leadingAnchor.constraintEqualToAnchor(view.leadingAnchor).active = true
            topAnchor.constraintEqualToAnchor(view.topAnchor).active = true
            widthAnchor.constraintEqualToAnchor(view.widthAnchor).active = true
            heightAnchor.constraintEqualToAnchor(view.heightAnchor).active = true
        }

        label = UILabel().apply {
            setFrame(CGRectMake(x = 10.0, y = 10.0, width = width - 100.0, height = 40.0))
            center = CGPointMake(x = width / 2, y = 40.0 )
            textAlignment = NSTextAlignmentCenter
            text = "Press OK"
            header.addSubview(this)
        }

        button = UIButton().apply {
            setFrame(CGRectMake(x = 10.0, y = height - 100.0, width = width - 100.0, height = 40.0))
            center = CGPointMake(x = width / 2, y = height / 2)
            backgroundColor = UIColor.blueColor
            setTitle("OK", forState = UIControlStateNormal)
            font = UIFont.fontWithName(fontName = font.fontName, size = 28.0)!!
            layer.borderWidth = 1.0
            layer.borderColor = UIColor.colorWithRed(0x47 / 255.0, 0x43 / 255.0, 0x70 / 255.0, 1.0).CGColor
            layer.masksToBounds = true
            addTarget(target = this@ViewController, action = NSSelectorFromString("buttonPressed"),
                    forControlEvents = UIControlEventTouchUpInside)
            header.addSubview(this)
        }

    }
}
