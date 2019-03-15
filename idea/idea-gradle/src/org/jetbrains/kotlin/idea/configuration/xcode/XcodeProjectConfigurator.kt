/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.xcode

import com.intellij.openapi.vfs.VirtualFile
import java.io.BufferedWriter

class XcodeProjectConfigurator {

    private fun VirtualFile.bufferedWriter() = getOutputStream(this).bufferedWriter()

    private val mppDirName = "app"

    fun createSkeleton(rootDir: VirtualFile) {
        val iosDir = rootDir.createChildDirectory(this, "iosApp")
        val sourceDir = iosDir.createChildDirectory(this, "iosApp")
        val storyboardDir = sourceDir.createChildDirectory(this, "Base.lproj")
        val testDir = iosDir.createChildDirectory(this, "iosAppTests")
        val projectDir = iosDir.createChildDirectory(this, "iosApp.xcodeproj")

        val tests = testDir.createChildData(this, "iosAppTests.swift").bufferedWriter()
        val testInfo = testDir.createChildData(this, "Info.plist").bufferedWriter()
        val appDelegate = sourceDir.createChildData(this, "AppDelegate.swift").bufferedWriter()
        val viewController = sourceDir.createChildData(this, "ViewController.swift").bufferedWriter()
        val sourceInfo = sourceDir.createChildData(this, "Info.plist").bufferedWriter()
        val launchScreen = storyboardDir.createChildData(this, "LaunchScreen.storyboard").bufferedWriter()
        val mainScreen = storyboardDir.createChildData(this, "Main.storyboard").bufferedWriter()
        val project = projectDir.createChildData(this, "project.pbxproj").bufferedWriter()

        try {
            tests.write(
                """
import XCTest
import $mppDirName

class iosAppTests: XCTestCase {
    func testExample() {
        assert(Sample().checkMe() == 7)
    }
}
                """.trimIndent()
            )
            appDelegate.write(
                """
import UIKit

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplicationLaunchOptionsKey: Any]?) -> Bool {
        return true
    }

    func applicationWillResignActive(_ application: UIApplication) {}

    func applicationDidEnterBackground(_ application: UIApplication) {}

    func applicationWillEnterForeground(_ application: UIApplication) {}

    func applicationDidBecomeActive(_ application: UIApplication) {}

    func applicationWillTerminate(_ application: UIApplication) {}
}
                """.trimIndent()
            )
            viewController.write(
                """
import UIKit
import $mppDirName

class ViewController: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        label.text = Proxy().proxyHello()
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }
    @IBOutlet weak var label: UILabel!
}
                """.trimIndent()
            )
            testInfo.write(
                """
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "https://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>CFBundleDevelopmentRegion</key>
	<string>${'$'}(DEVELOPMENT_LANGUAGE)</string>
	<key>CFBundleExecutable</key>
	<string>${'$'}(EXECUTABLE_NAME)</string>
	<key>CFBundleIdentifier</key>
	<string>${'$'}(PRODUCT_BUNDLE_IDENTIFIER)</string>
	<key>CFBundleInfoDictionaryVersion</key>
	<string>6.0</string>
	<key>CFBundleName</key>
	<string>${'$'}(PRODUCT_NAME)</string>
	<key>CFBundlePackageType</key>
	<string>BNDL</string>
	<key>CFBundleShortVersionString</key>
	<string>1.0</string>
	<key>CFBundleVersion</key>
	<string>1</string>
</dict>
</plist>
                """.trimIndent()
            )
            sourceInfo.write(
                """
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "https://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>CFBundleDevelopmentRegion</key>
	<string>${'$'}(DEVELOPMENT_LANGUAGE)</string>
	<key>CFBundleExecutable</key>
	<string>${'$'}(EXECUTABLE_NAME)</string>
	<key>CFBundleIdentifier</key>
	<string>${'$'}(PRODUCT_BUNDLE_IDENTIFIER)</string>
	<key>CFBundleInfoDictionaryVersion</key>
	<string>6.0</string>
	<key>CFBundleName</key>
	<string>${'$'}(PRODUCT_NAME)</string>
	<key>CFBundlePackageType</key>
	<string>APPL</string>
	<key>CFBundleShortVersionString</key>
	<string>1.0</string>
	<key>CFBundleVersion</key>
	<string>1</string>
	<key>LSRequiresIPhoneOS</key>
	<true/>
	<key>UILaunchStoryboardName</key>
	<string>LaunchScreen</string>
	<key>UIMainStoryboardFile</key>
	<string>Main</string>
	<key>UIRequiredDeviceCapabilities</key>
	<array>
		<string>armv7</string>
	</array>
	<key>UISupportedInterfaceOrientations</key>
	<array>
		<string>UIInterfaceOrientationPortrait</string>
		<string>UIInterfaceOrientationLandscapeLeft</string>
		<string>UIInterfaceOrientationLandscapeRight</string>
	</array>
	<key>UISupportedInterfaceOrientations~ipad</key>
	<array>
		<string>UIInterfaceOrientationPortrait</string>
		<string>UIInterfaceOrientationPortraitUpsideDown</string>
		<string>UIInterfaceOrientationLandscapeLeft</string>
		<string>UIInterfaceOrientationLandscapeRight</string>
	</array>
</dict>
</plist>
                """.trimIndent()
            )
            launchScreen.write(
                """
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<document type="com.apple.InterfaceBuilder3.CocoaTouch.Storyboard.XIB" version="3.0" toolsVersion="13122.16" systemVersion="17A277" targetRuntime="iOS.CocoaTouch" propertyAccessControl="none" useAutolayout="YES" launchScreen="YES" useTraitCollections="YES" useSafeAreas="YES" colorMatched="YES" initialViewController="01J-lp-oVM">
    <dependencies>
        <plugIn identifier="com.apple.InterfaceBuilder.IBCocoaTouchPlugin" version="13104.12"/>
        <capability name="Safe area layout guides" minToolsVersion="9.0"/>
        <capability name="documents saved in the Xcode 8 format" minToolsVersion="8.0"/>
    </dependencies>
    <scenes>
        <!--View Controller-->
        <scene sceneID="EHf-IW-A2E">
            <objects>
                <viewController id="01J-lp-oVM" sceneMemberID="viewController">
                    <view key="view" contentMode="scaleToFill" id="Ze5-6b-2t3">
                        <rect key="frame" x="0.0" y="0.0" width="375" height="667"/>
                        <autoresizingMask key="autoresizingMask" widthSizable="YES" heightSizable="YES"/>
                        <color key="backgroundColor" red="1" green="1" blue="1" alpha="1" colorSpace="custom" customColorSpace="sRGB"/>
                        <viewLayoutGuide key="safeArea" id="6Tk-OE-BBY"/>
                    </view>
                </viewController>
                <placeholder placeholderIdentifier="IBFirstResponder" id="iYj-Kq-Ea1" userLabel="First Responder" sceneMemberID="firstResponder"/>
            </objects>
            <point key="canvasLocation" x="53" y="375"/>
        </scene>
    </scenes>
</document>
                """.trimIndent()
            )
            mainScreen.write(
                """
<?xml version="1.0" encoding="UTF-8"?>
<document type="com.apple.InterfaceBuilder3.CocoaTouch.Storyboard.XIB" version="3.0" toolsVersion="14113" targetRuntime="iOS.CocoaTouch" propertyAccessControl="none" useAutolayout="YES" useTraitCollections="YES" useSafeAreas="YES" colorMatched="YES" initialViewController="BYZ-38-t0r">
    <device id="retina4_7" orientation="portrait">
        <adaptation id="fullscreen"/>
    </device>
    <dependencies>
        <deployment identifier="iOS"/>
        <plugIn identifier="com.apple.InterfaceBuilder.IBCocoaTouchPlugin" version="14088"/>
        <capability name="Safe area layout guides" minToolsVersion="9.0"/>
        <capability name="documents saved in the Xcode 8 format" minToolsVersion="8.0"/>
    </dependencies>
    <scenes>
        <!--View Controller-->
        <scene sceneID="tne-QT-ifu">
            <objects>
                <viewController id="BYZ-38-t0r" customClass="ViewController" customModule="iosApp" customModuleProvider="target" sceneMemberID="viewController">
                    <view key="view" contentMode="scaleToFill" id="8bC-Xf-vdC">
                        <rect key="frame" x="0.0" y="0.0" width="375" height="667"/>
                        <autoresizingMask key="autoresizingMask" widthSizable="YES" heightSizable="YES"/>
                        <subviews>
                            <label opaque="NO" userInteractionEnabled="NO" contentMode="left" horizontalHuggingPriority="251" verticalHuggingPriority="251" fixedFrame="YES" text="Label" textAlignment="natural" lineBreakMode="wordWrap" numberOfLines="3" baselineAdjustment="alignBaselines" adjustsFontSizeToFit="NO" translatesAutoresizingMaskIntoConstraints="NO" id="Cak-wb-syn">
                                <rect key="frame" x="113" y="120" width="216" height="193"/>
                                <autoresizingMask key="autoresizingMask" flexibleMaxX="YES" flexibleMaxY="YES"/>
                                <fontDescription key="fontDescription" type="system" pointSize="17"/>
                                <nil key="textColor"/>
                                <nil key="highlightedColor"/>
                            </label>
                        </subviews>
                        <color key="backgroundColor" red="1" green="1" blue="1" alpha="1" colorSpace="custom" customColorSpace="sRGB"/>
                        <viewLayoutGuide key="safeArea" id="6Tk-OE-BBY"/>
                    </view>
                    <connections>
                        <outlet property="label" destination="Cak-wb-syn" id="pzW-Xf-pbM"/>
                    </connections>
                </viewController>
                <placeholder placeholderIdentifier="IBFirstResponder" id="dkx-z0-nzr" sceneMemberID="firstResponder"/>
            </objects>
            <point key="canvasLocation" x="117.59999999999999" y="118.29085457271366"/>
        </scene>
    </scenes>
</document>
                """.trimIndent()
            )
            project.fillProject()
        } finally {
            listOf(
                tests, testInfo, appDelegate, viewController, sourceInfo, launchScreen, mainScreen, project
            ).forEach(BufferedWriter::close)
        }
    }

    private fun BufferedWriter.fillProject() {
        write(
            """
// !${'$'}*UTF8*${'$'}!
{
	archiveVersion = 1;
	classes = {
	};
	objectVersion = 50;
	objects = {

/* Begin PBXBuildFile section */
		F861D7E1207FA40F0085E80D /* AppDelegate.swift in Sources */ = {isa = PBXBuildFile; fileRef = F861D7E0207FA40F0085E80D /* AppDelegate.swift */; };
		F861D7E3207FA40F0085E80D /* ViewController.swift in Sources */ = {isa = PBXBuildFile; fileRef = F861D7E2207FA40F0085E80D /* ViewController.swift */; };
		F861D7E6207FA40F0085E80D /* Main.storyboard in Resources */ = {isa = PBXBuildFile; fileRef = F861D7E4207FA40F0085E80D /* Main.storyboard */; };
		F861D7EB207FA4100085E80D /* LaunchScreen.storyboard in Resources */ = {isa = PBXBuildFile; fileRef = F861D7E9207FA4100085E80D /* LaunchScreen.storyboard */; };
		F861D7F6207FA4100085E80D /* iosAppTests.swift in Sources */ = {isa = PBXBuildFile; fileRef = F861D7F5207FA4100085E80D /* iosAppTests.swift */; };
		F861D80C207FA4200085E80D /* $mppDirName.framework in Frameworks */ = {isa = PBXBuildFile; fileRef = F861D805207FA4200085E80D /* $mppDirName.framework */; };
		F861D80D207FA4200085E80D /* $mppDirName.framework in Embed Frameworks */ = {isa = PBXBuildFile; fileRef = F861D805207FA4200085E80D /* $mppDirName.framework */; settings = {ATTRIBUTES = (CodeSignOnCopy, RemoveHeadersOnCopy, ); }; };
/* End PBXBuildFile section */

/* Begin PBXContainerItemProxy section */
		F861D7F2207FA4100085E80D /* PBXContainerItemProxy */ = {
			isa = PBXContainerItemProxy;
			containerPortal = F861D7D5207FA40F0085E80D /* Project object */;
			proxyType = 1;
			remoteGlobalIDString = F861D7DC207FA40F0085E80D;
			remoteInfo = iosApp;
		};
		F861D80A207FA4200085E80D /* PBXContainerItemProxy */ = {
			isa = PBXContainerItemProxy;
			containerPortal = F861D7D5207FA40F0085E80D /* Project object */;
			proxyType = 1;
			remoteGlobalIDString = F861D804207FA4200085E80D;
			remoteInfo = $mppDirName;
		};
/* End PBXContainerItemProxy section */

/* Begin PBXCopyFilesBuildPhase section */
		F861D811207FA4200085E80D /* Embed Frameworks */ = {
			isa = PBXCopyFilesBuildPhase;
			buildActionMask = 2147483647;
			dstPath = "";
			dstSubfolderSpec = 10;
			files = (
				F861D80D207FA4200085E80D /* $mppDirName.framework in Embed Frameworks */,
			);
			name = "Embed Frameworks";
			runOnlyForDeploymentPostprocessing = 0;
		};
/* End PBXCopyFilesBuildPhase section */

/* Begin PBXFileReference section */
		F861D7DD207FA40F0085E80D /* iosApp.$mppDirName */ = {isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = iosApp.$mppDirName; sourceTree = BUILT_PRODUCTS_DIR; };
		F861D7E0207FA40F0085E80D /* AppDelegate.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = AppDelegate.swift; sourceTree = "<group>"; };
		F861D7E2207FA40F0085E80D /* ViewController.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = ViewController.swift; sourceTree = "<group>"; };
		F861D7E5207FA40F0085E80D /* Base */ = {isa = PBXFileReference; lastKnownFileType = file.storyboard; name = Base; path = Base.lproj/Main.storyboard; sourceTree = "<group>"; };
		F861D7EA207FA4100085E80D /* Base */ = {isa = PBXFileReference; lastKnownFileType = file.storyboard; name = Base; path = Base.lproj/LaunchScreen.storyboard; sourceTree = "<group>"; };
		F861D7EC207FA4100085E80D /* Info.plist */ = {isa = PBXFileReference; lastKnownFileType = text.plist.xml; path = Info.plist; sourceTree = "<group>"; };
		F861D7F1207FA4100085E80D /* iosAppTests.xctest */ = {isa = PBXFileReference; explicitFileType = wrapper.cfbundle; includeInIndex = 0; path = iosAppTests.xctest; sourceTree = BUILT_PRODUCTS_DIR; };
		F861D7F5207FA4100085E80D /* iosAppTests.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = iosAppTests.swift; sourceTree = "<group>"; };
		F861D7F7207FA4100085E80D /* Info.plist */ = {isa = PBXFileReference; lastKnownFileType = text.plist.xml; path = Info.plist; sourceTree = "<group>"; };
		F861D805207FA4200085E80D /* $mppDirName.framework */ = {isa = PBXFileReference; explicitFileType = wrapper.framework; includeInIndex = 0; path = $mppDirName.framework; sourceTree = BUILT_PRODUCTS_DIR; };
		F861D813207FA4520085E80D /* $mppDirName */ = {isa = PBXFileReference; lastKnownFileType = folder; name = $mppDirName; path = ../$mppDirName; sourceTree = "<group>"; };
/* End PBXFileReference section */

/* Begin PBXFrameworksBuildPhase section */
		F861D7DA207FA40F0085E80D /* Frameworks */ = {
			isa = PBXFrameworksBuildPhase;
			buildActionMask = 2147483647;
			files = (
				F861D80C207FA4200085E80D /* $mppDirName.framework in Frameworks */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
		F861D7EE207FA4100085E80D /* Frameworks */ = {
			isa = PBXFrameworksBuildPhase;
			buildActionMask = 2147483647;
			files = (
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
/* End PBXFrameworksBuildPhase section */

/* Begin PBXGroup section */
		F861D7D4207FA40F0085E80D = {
			isa = PBXGroup;
			children = (
				F861D813207FA4520085E80D /* $mppDirName */,
				F861D7DF207FA40F0085E80D /* iosApp */,
				F861D7F4207FA4100085E80D /* iosAppTests */,
				F861D7DE207FA40F0085E80D /* Products */,
			);
			sourceTree = "<group>";
		};
		F861D7DE207FA40F0085E80D /* Products */ = {
			isa = PBXGroup;
			children = (
				F861D7DD207FA40F0085E80D /* iosApp.$mppDirName */,
				F861D7F1207FA4100085E80D /* iosAppTests.xctest */,
				F861D805207FA4200085E80D /* $mppDirName.framework */,
			);
			name = Products;
			sourceTree = "<group>";
		};
		F861D7DF207FA40F0085E80D /* iosApp */ = {
			isa = PBXGroup;
			children = (
				F861D7E0207FA40F0085E80D /* AppDelegate.swift */,
				F861D7E2207FA40F0085E80D /* ViewController.swift */,
				F861D7E4207FA40F0085E80D /* Main.storyboard */,
				F861D7E9207FA4100085E80D /* LaunchScreen.storyboard */,
				F861D7EC207FA4100085E80D /* Info.plist */,
			);
			path = iosApp;
			sourceTree = "<group>";
		};
		F861D7F4207FA4100085E80D /* iosAppTests */ = {
			isa = PBXGroup;
			children = (
				F861D7F5207FA4100085E80D /* iosAppTests.swift */,
				F861D7F7207FA4100085E80D /* Info.plist */,
			);
			path = iosAppTests;
			sourceTree = "<group>";
		};
/* End PBXGroup section */

/* Begin PBXNativeTarget section */
		F861D7DC207FA40F0085E80D /* iosApp */ = {
			isa = PBXNativeTarget;
			buildConfigurationList = F861D7FA207FA4100085E80D /* Build configuration list for PBXNativeTarget "iosApp" */;
			buildPhases = (
				F861D7D9207FA40F0085E80D /* Sources */,
				F861D7DA207FA40F0085E80D /* Frameworks */,
				F861D7DB207FA40F0085E80D /* Resources */,
				F861D811207FA4200085E80D /* Embed Frameworks */,
			);
			buildRules = (
			);
			dependencies = (
				F861D80B207FA4200085E80D /* PBXTargetDependency */,
			);
			name = iosApp;
			productName = iosApp;
			productReference = F861D7DD207FA40F0085E80D /* iosApp.$mppDirName */;
			productType = "com.apple.product-type.application";
		};
		F861D7F0207FA4100085E80D /* iosAppTests */ = {
			isa = PBXNativeTarget;
			buildConfigurationList = F861D7FD207FA4100085E80D /* Build configuration list for PBXNativeTarget "iosAppTests" */;
			buildPhases = (
				F861D7ED207FA4100085E80D /* Sources */,
				F861D7EE207FA4100085E80D /* Frameworks */,
				F861D7EF207FA4100085E80D /* Resources */,
			);
			buildRules = (
			);
			dependencies = (
				F861D7F3207FA4100085E80D /* PBXTargetDependency */,
			);
			name = iosAppTests;
			productName = iosAppTests;
			productReference = F861D7F1207FA4100085E80D /* iosAppTests.xctest */;
			productType = "com.apple.product-type.bundle.unit-test";
		};
		F861D804207FA4200085E80D /* $mppDirName */ = {
			isa = PBXNativeTarget;
			buildConfigurationList = F861D80E207FA4200085E80D /* Build configuration list for PBXNativeTarget "$mppDirName" */;
			buildPhases = (
				F861D812207FA4320085E80D /* ShellScript */,
			);
			buildRules = (
			);
			dependencies = (
			);
			name = $mppDirName;
			productName = $mppDirName;
			productReference = F861D805207FA4200085E80D /* $mppDirName.framework */;
			productType = "com.apple.product-type.framework";
		};
/* End PBXNativeTarget section */

/* Begin PBXProject section */
		F861D7D5207FA40F0085E80D /* Project object */ = {
			isa = PBXProject;
			attributes = {
				LastSwiftUpdateCheck = 0930;
				LastUpgradeCheck = 0930;
				TargetAttributes = {
					F861D7DC207FA40F0085E80D = {
						CreatedOnToolsVersion = 9.3;
					};
					F861D7F0207FA4100085E80D = {
						CreatedOnToolsVersion = 9.3;
						TestTargetID = F861D7DC207FA40F0085E80D;
					};
					F861D804207FA4200085E80D = {
						CreatedOnToolsVersion = 9.3;
					};
				};
			};
			buildConfigurationList = F861D7D8207FA40F0085E80D /* Build configuration list for PBXProject "iosApp" */;
			compatibilityVersion = "Xcode 9.3";
			developmentRegion = en;
			hasScannedForEncodings = 0;
			knownRegions = (
				en,
				Base,
			);
			mainGroup = F861D7D4207FA40F0085E80D;
			productRefGroup = F861D7DE207FA40F0085E80D /* Products */;
			projectDirPath = "";
			projectRoot = "";
			targets = (
				F861D7DC207FA40F0085E80D /* iosApp */,
				F861D7F0207FA4100085E80D /* iosAppTests */,
				F861D804207FA4200085E80D /* $mppDirName */,
			);
		};
/* End PBXProject section */

/* Begin PBXResourcesBuildPhase section */
		F861D7DB207FA40F0085E80D /* Resources */ = {
			isa = PBXResourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
				F861D7EB207FA4100085E80D /* LaunchScreen.storyboard in Resources */,
				F861D7E6207FA40F0085E80D /* Main.storyboard in Resources */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
		F861D7EF207FA4100085E80D /* Resources */ = {
			isa = PBXResourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
/* End PBXResourcesBuildPhase section */

/* Begin PBXShellScriptBuildPhase section */
		F861D812207FA4320085E80D /* ShellScript */ = {
			isa = PBXShellScriptBuildPhase;
			buildActionMask = 2147483647;
			files = (
			);
			inputPaths = (
			);
			outputPaths = (
			);
			runOnlyForDeploymentPostprocessing = 0;
			shellPath = /bin/sh;
			shellScript = "\"${"$"}SRCROOT/../gradlew\" -p \"${"$"}SRCROOT/../$mppDirName\" copyFramework \\\n-Pconfiguration.build.dir=\"${"$"}CONFIGURATION_BUILD_DIR\"          \\\n-Pkotlin.build.type=\"${"$"}KOTLIN_BUILD_TYPE\"                      \\\n-Pkotlin.target=\"${"$"}KOTLIN_TARGET\"";
		};
/* End PBXShellScriptBuildPhase section */

/* Begin PBXSourcesBuildPhase section */
		F861D7D9207FA40F0085E80D /* Sources */ = {
			isa = PBXSourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
				F861D7E3207FA40F0085E80D /* ViewController.swift in Sources */,
				F861D7E1207FA40F0085E80D /* AppDelegate.swift in Sources */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
		F861D7ED207FA4100085E80D /* Sources */ = {
			isa = PBXSourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
				F861D7F6207FA4100085E80D /* iosAppTests.swift in Sources */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
/* End PBXSourcesBuildPhase section */

/* Begin PBXTargetDependency section */
		F861D7F3207FA4100085E80D /* PBXTargetDependency */ = {
			isa = PBXTargetDependency;
			target = F861D7DC207FA40F0085E80D /* iosApp */;
			targetProxy = F861D7F2207FA4100085E80D /* PBXContainerItemProxy */;
		};
		F861D80B207FA4200085E80D /* PBXTargetDependency */ = {
			isa = PBXTargetDependency;
			target = F861D804207FA4200085E80D /* $mppDirName */;
			targetProxy = F861D80A207FA4200085E80D /* PBXContainerItemProxy */;
		};
/* End PBXTargetDependency section */

/* Begin PBXVariantGroup section */
		F861D7E4207FA40F0085E80D /* Main.storyboard */ = {
			isa = PBXVariantGroup;
			children = (
				F861D7E5207FA40F0085E80D /* Base */,
			);
			name = Main.storyboard;
			sourceTree = "<group>";
		};
		F861D7E9207FA4100085E80D /* LaunchScreen.storyboard */ = {
			isa = PBXVariantGroup;
			children = (
				F861D7EA207FA4100085E80D /* Base */,
			);
			name = LaunchScreen.storyboard;
			sourceTree = "<group>";
		};
/* End PBXVariantGroup section */

/* Begin XCBuildConfiguration section */
		F861D7F8207FA4100085E80D /* Debug */ = {
			isa = XCBuildConfiguration;
			buildSettings = {
				ALWAYS_SEARCH_USER_PATHS = NO;
				CLANG_ANALYZER_NONNULL = YES;
				CLANG_ANALYZER_NUMBER_OBJECT_CONVERSION = YES_AGGRESSIVE;
				CLANG_CXX_LANGUAGE_STANDARD = "gnu++14";
				CLANG_CXX_LIBRARY = "libc++";
				CLANG_ENABLE_MODULES = YES;
				CLANG_ENABLE_OBJC_ARC = YES;
				CLANG_ENABLE_OBJC_WEAK = YES;
				CLANG_WARN_BLOCK_CAPTURE_AUTORELEASING = YES;
				CLANG_WARN_BOOL_CONVERSION = YES;
				CLANG_WARN_COMMA = YES;
				CLANG_WARN_CONSTANT_CONVERSION = YES;
				CLANG_WARN_DEPRECATED_OBJC_IMPLEMENTATIONS = YES;
				CLANG_WARN_DIRECT_OBJC_ISA_USAGE = YES_ERROR;
				CLANG_WARN_DOCUMENTATION_COMMENTS = YES;
				CLANG_WARN_EMPTY_BODY = YES;
				CLANG_WARN_ENUM_CONVERSION = YES;
				CLANG_WARN_INFINITE_RECURSION = YES;
				CLANG_WARN_INT_CONVERSION = YES;
				CLANG_WARN_NON_LITERAL_NULL_CONVERSION = YES;
				CLANG_WARN_OBJC_IMPLICIT_RETAIN_SELF = YES;
				CLANG_WARN_OBJC_LITERAL_CONVERSION = YES;
				CLANG_WARN_OBJC_ROOT_CLASS = YES_ERROR;
				CLANG_WARN_RANGE_LOOP_ANALYSIS = YES;
				CLANG_WARN_STRICT_PROTOTYPES = YES;
				CLANG_WARN_SUSPICIOUS_MOVE = YES;
				CLANG_WARN_UNGUARDED_AVAILABILITY = YES_AGGRESSIVE;
				CLANG_WARN_UNREACHABLE_CODE = YES;
				CLANG_WARN__DUPLICATE_METHOD_MATCH = YES;
				CODE_SIGN_IDENTITY = "iPhone Developer";
				COPY_PHASE_STRIP = NO;
				DEBUG_INFORMATION_FORMAT = dwarf;
				ENABLE_BITCODE = NO;
				ENABLE_STRICT_OBJC_MSGSEND = YES;
				ENABLE_TESTABILITY = YES;
				GCC_C_LANGUAGE_STANDARD = gnu11;
				GCC_DYNAMIC_NO_PIC = NO;
				GCC_NO_COMMON_BLOCKS = YES;
				GCC_OPTIMIZATION_LEVEL = 0;
				GCC_PREPROCESSOR_DEFINITIONS = (
					"DEBUG=1",
					"${'$'}(inherited)",
				);
				GCC_WARN_64_TO_32_BIT_CONVERSION = YES;
				GCC_WARN_ABOUT_RETURN_TYPE = YES_ERROR;
				GCC_WARN_UNDECLARED_SELECTOR = YES;
				GCC_WARN_UNINITIALIZED_AUTOS = YES_AGGRESSIVE;
				GCC_WARN_UNUSED_FUNCTION = YES;
				GCC_WARN_UNUSED_VARIABLE = YES;
				IPHONEOS_DEPLOYMENT_TARGET = 11.3;
				MTL_ENABLE_DEBUG_INFO = YES;
				ONLY_ACTIVE_ARCH = YES;
				OTHER_LDFLAGS = "-v";
				SDKROOT = iphoneos;
				SWIFT_ACTIVE_COMPILATION_CONDITIONS = DEBUG;
				SWIFT_OPTIMIZATION_LEVEL = "-Onone";
			};
			name = Debug;
		};
		F861D7F9207FA4100085E80D /* Release */ = {
			isa = XCBuildConfiguration;
			buildSettings = {
				ALWAYS_SEARCH_USER_PATHS = NO;
				CLANG_ANALYZER_NONNULL = YES;
				CLANG_ANALYZER_NUMBER_OBJECT_CONVERSION = YES_AGGRESSIVE;
				CLANG_CXX_LANGUAGE_STANDARD = "gnu++14";
				CLANG_CXX_LIBRARY = "libc++";
				CLANG_ENABLE_MODULES = YES;
				CLANG_ENABLE_OBJC_ARC = YES;
				CLANG_ENABLE_OBJC_WEAK = YES;
				CLANG_WARN_BLOCK_CAPTURE_AUTORELEASING = YES;
				CLANG_WARN_BOOL_CONVERSION = YES;
				CLANG_WARN_COMMA = YES;
				CLANG_WARN_CONSTANT_CONVERSION = YES;
				CLANG_WARN_DEPRECATED_OBJC_IMPLEMENTATIONS = YES;
				CLANG_WARN_DIRECT_OBJC_ISA_USAGE = YES_ERROR;
				CLANG_WARN_DOCUMENTATION_COMMENTS = YES;
				CLANG_WARN_EMPTY_BODY = YES;
				CLANG_WARN_ENUM_CONVERSION = YES;
				CLANG_WARN_INFINITE_RECURSION = YES;
				CLANG_WARN_INT_CONVERSION = YES;
				CLANG_WARN_NON_LITERAL_NULL_CONVERSION = YES;
				CLANG_WARN_OBJC_IMPLICIT_RETAIN_SELF = YES;
				CLANG_WARN_OBJC_LITERAL_CONVERSION = YES;
				CLANG_WARN_OBJC_ROOT_CLASS = YES_ERROR;
				CLANG_WARN_RANGE_LOOP_ANALYSIS = YES;
				CLANG_WARN_STRICT_PROTOTYPES = YES;
				CLANG_WARN_SUSPICIOUS_MOVE = YES;
				CLANG_WARN_UNGUARDED_AVAILABILITY = YES_AGGRESSIVE;
				CLANG_WARN_UNREACHABLE_CODE = YES;
				CLANG_WARN__DUPLICATE_METHOD_MATCH = YES;
				CODE_SIGN_IDENTITY = "iPhone Developer";
				COPY_PHASE_STRIP = NO;
				DEBUG_INFORMATION_FORMAT = "dwarf-with-dsym";
				ENABLE_BITCODE = NO;
				ENABLE_NS_ASSERTIONS = NO;
				ENABLE_STRICT_OBJC_MSGSEND = YES;
				GCC_C_LANGUAGE_STANDARD = gnu11;
				GCC_NO_COMMON_BLOCKS = YES;
				GCC_WARN_64_TO_32_BIT_CONVERSION = YES;
				GCC_WARN_ABOUT_RETURN_TYPE = YES_ERROR;
				GCC_WARN_UNDECLARED_SELECTOR = YES;
				GCC_WARN_UNINITIALIZED_AUTOS = YES_AGGRESSIVE;
				GCC_WARN_UNUSED_FUNCTION = YES;
				GCC_WARN_UNUSED_VARIABLE = YES;
				IPHONEOS_DEPLOYMENT_TARGET = 11.3;
				MTL_ENABLE_DEBUG_INFO = NO;
				OTHER_LDFLAGS = "-v";
				SDKROOT = iphoneos;
				SWIFT_COMPILATION_MODE = wholemodule;
				SWIFT_OPTIMIZATION_LEVEL = "-O";
				VALIDATE_PRODUCT = YES;
			};
			name = Release;
		};
		F861D7FB207FA4100085E80D /* Debug */ = {
			isa = XCBuildConfiguration;
			buildSettings = {
				ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
				CODE_SIGN_STYLE = Automatic;
				ENABLE_BITCODE = NO;
				INFOPLIST_FILE = iosApp/Info.plist;
				LD_RUNPATH_SEARCH_PATHS = (
					"${'$'}(inherited)",
					"@executable_path/Frameworks",
				);
				OTHER_LDFLAGS = "-v";
				PRODUCT_BUNDLE_IDENTIFIER = com.example.iosApp;
				PRODUCT_NAME = "${'$'}(TARGET_NAME)";
				SWIFT_VERSION = 4.0;
				TARGETED_DEVICE_FAMILY = "1,2";
				VALID_ARCHS = "arm64 armv7";
			};
			name = Debug;
		};
		F861D7FC207FA4100085E80D /* Release */ = {
			isa = XCBuildConfiguration;
			buildSettings = {
				ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
				CODE_SIGN_STYLE = Automatic;
				ENABLE_BITCODE = NO;
				INFOPLIST_FILE = iosApp/Info.plist;
				LD_RUNPATH_SEARCH_PATHS = (
					"${'$'}(inherited)",
					"@executable_path/Frameworks",
				);
				OTHER_LDFLAGS = "-v";
				PRODUCT_BUNDLE_IDENTIFIER = com.example.iosApp;
				PRODUCT_NAME = "${'$'}(TARGET_NAME)";
				SWIFT_VERSION = 4.0;
				TARGETED_DEVICE_FAMILY = "1,2";
				VALID_ARCHS = "arm64 armv7";
			};
			name = Release;
		};
		F861D7FE207FA4100085E80D /* Debug */ = {
			isa = XCBuildConfiguration;
			buildSettings = {
				ALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES = YES;
				BUNDLE_LOADER = "${'$'}(TEST_HOST)";
				CODE_SIGN_STYLE = Automatic;
				INFOPLIST_FILE = iosAppTests/Info.plist;
				LD_RUNPATH_SEARCH_PATHS = (
					"${'$'}(inherited)",
					"@executable_path/Frameworks",
					"@loader_path/Frameworks",
				);
				PRODUCT_BUNDLE_IDENTIFIER = com.example.iosAppTests;
				PRODUCT_NAME = "${'$'}(TARGET_NAME)";
				SWIFT_VERSION = 4.0;
				TARGETED_DEVICE_FAMILY = "1,2";
				TEST_HOST = "${'$'}(BUILT_PRODUCTS_DIR)/iosApp.$mppDirName/iosApp";
			};
			name = Debug;
		};
		F861D7FF207FA4100085E80D /* Release */ = {
			isa = XCBuildConfiguration;
			buildSettings = {
				ALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES = YES;
				BUNDLE_LOADER = "${'$'}(TEST_HOST)";
				CODE_SIGN_STYLE = Automatic;
				INFOPLIST_FILE = iosAppTests/Info.plist;
				LD_RUNPATH_SEARCH_PATHS = (
					"${'$'}(inherited)",
					"@executable_path/Frameworks",
					"@loader_path/Frameworks",
				);
				PRODUCT_BUNDLE_IDENTIFIER = com.example.iosAppTests;
				PRODUCT_NAME = "${'$'}(TARGET_NAME)";
				SWIFT_VERSION = 4.0;
				TARGETED_DEVICE_FAMILY = "1,2";
				TEST_HOST = "${'$'}(BUILT_PRODUCTS_DIR)/iosApp.$mppDirName/iosApp";
			};
			name = Release;
		};
		F861D80F207FA4200085E80D /* Debug */ = {
			isa = XCBuildConfiguration;
			buildSettings = {
				CODE_SIGN_IDENTITY = "";
				CODE_SIGN_STYLE = Automatic;
				CURRENT_PROJECT_VERSION = 1;
				DEFINES_MODULE = YES;
				DYLIB_COMPATIBILITY_VERSION = 1;
				DYLIB_CURRENT_VERSION = 1;
				DYLIB_INSTALL_NAME_BASE = "@rpath";
				ENABLE_BITCODE = NO;
				INSTALL_PATH = "${'$'}(LOCAL_LIBRARY_DIR)/Frameworks";
				KOTLIN_BUILD_TYPE = DEBUG;
				KOTLIN_TARGET = "";
				"KOTLIN_TARGET[sdk=iphoneos*]" = ios;
				"KOTLIN_TARGET[sdk=iphonesimulator*]" = ios;
				LD_RUNPATH_SEARCH_PATHS = (
					"${'$'}(inherited)",
					"@executable_path/Frameworks",
					"@loader_path/Frameworks",
				);
				PRODUCT_BUNDLE_IDENTIFIER = com.example.$mppDirName;
				PRODUCT_NAME = "${'$'}(TARGET_NAME:c99extidentifier)";
				SKIP_INSTALL = YES;
				SWIFT_VERSION = 4.0;
				TARGETED_DEVICE_FAMILY = "1,2";
				VERSIONING_SYSTEM = "apple-generic";
				VERSION_INFO_PREFIX = "";
			};
			name = Debug;
		};
		F861D810207FA4200085E80D /* Release */ = {
			isa = XCBuildConfiguration;
			buildSettings = {
				CODE_SIGN_IDENTITY = "";
				CODE_SIGN_STYLE = Automatic;
				CURRENT_PROJECT_VERSION = 1;
				DEFINES_MODULE = YES;
				DYLIB_COMPATIBILITY_VERSION = 1;
				DYLIB_CURRENT_VERSION = 1;
				DYLIB_INSTALL_NAME_BASE = "@rpath";
				ENABLE_BITCODE = NO;
				INSTALL_PATH = "${'$'}(LOCAL_LIBRARY_DIR)/Frameworks";
				KOTLIN_BUILD_TYPE = RELEASE;
				KOTLIN_TARGET = "";
				"KOTLIN_TARGET[sdk=iphoneos*]" = ios;
				"KOTLIN_TARGET[sdk=iphonesimulator*]" = ios;
				LD_RUNPATH_SEARCH_PATHS = (
					"${'$'}(inherited)",
					"@executable_path/Frameworks",
					"@loader_path/Frameworks",
				);
				PRODUCT_BUNDLE_IDENTIFIER = com.example.$mppDirName;
				PRODUCT_NAME = "${'$'}(TARGET_NAME:c99extidentifier)";
				SKIP_INSTALL = YES;
				SWIFT_VERSION = 4.0;
				TARGETED_DEVICE_FAMILY = 1;
				VERSIONING_SYSTEM = "apple-generic";
				VERSION_INFO_PREFIX = "";
			};
			name = Release;
		};
/* End XCBuildConfiguration section */

/* Begin XCConfigurationList section */
		F861D7D8207FA40F0085E80D /* Build configuration list for PBXProject "iosApp" */ = {
			isa = XCConfigurationList;
			buildConfigurations = (
				F861D7F8207FA4100085E80D /* Debug */,
				F861D7F9207FA4100085E80D /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		};
		F861D7FA207FA4100085E80D /* Build configuration list for PBXNativeTarget "iosApp" */ = {
			isa = XCConfigurationList;
			buildConfigurations = (
				F861D7FB207FA4100085E80D /* Debug */,
				F861D7FC207FA4100085E80D /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		};
		F861D7FD207FA4100085E80D /* Build configuration list for PBXNativeTarget "iosAppTests" */ = {
			isa = XCConfigurationList;
			buildConfigurations = (
				F861D7FE207FA4100085E80D /* Debug */,
				F861D7FF207FA4100085E80D /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		};
		F861D80E207FA4200085E80D /* Build configuration list for PBXNativeTarget "$mppDirName" */ = {
			isa = XCConfigurationList;
			buildConfigurations = (
				F861D80F207FA4200085E80D /* Debug */,
				F861D810207FA4200085E80D /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		};
/* End XCConfigurationList section */
	};
	rootObject = F861D7D5207FA40F0085E80D /* Project object */;
}
            """.trimIndent()
        )
    }
}