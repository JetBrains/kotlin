/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.klib

import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.backend.common.serialization.cityHash128
import org.jetbrains.kotlin.backend.common.serialization.cityHash128WithSeed
import org.jetbrains.kotlin.backend.common.serialization.Hash128Bits
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase

class CityHashTest : KtUsefulTestCase() {
    private fun assert64BitHash(data: String, expectedHash: ULong) {
        assertEquals(cityHash64(data.toByteArray()), expectedHash)
    }

    private fun assert128BitHash(data: String, expectedLowBytes: ULong, expectedHighBytes: ULong) {
        assertEquals(cityHash128(data.toByteArray()), Hash128Bits(expectedLowBytes, expectedHighBytes))
    }

    fun testSimple64() {
        assert64BitHash("a", 12917804110809363939UL)
        assert64BitHash("abc", 2640714258260161385UL)
        assert64BitHash("a".repeat(20), 15065261471293997870UL)
        assert64BitHash("b".repeat(50), 1117739638843767388UL)
        assert64BitHash("Hello World!".repeat(40), 9211247178963271761UL)
    }

    fun testSimple128() {
        assert128BitHash("a", 7969074168056553668UL, 5955762262185338209UL)
        assert128BitHash("abc", 4143508125394299908UL, 11566915719555882565UL)

        assert128BitHash("a".repeat(15), 4649666081741216883UL, 11336931969441833971UL)
        assert128BitHash("a".repeat(16), 14694504640136803685UL, 15089679700857083186UL)
        assert128BitHash("a".repeat(17), 12343703380141332181UL, 12834902385258119372UL)
        assert128BitHash("a".repeat(20), 7859379477173421491UL, 12195612906319506123UL)

        assert128BitHash("b".repeat(35), 7419967890930857011UL, 2921906986518450892UL)
        assert128BitHash("b".repeat(36), 7387272621743901937UL, 6525889884051439248UL)
        assert128BitHash("b".repeat(37), 12608089518518791205UL, 16206682210629811869UL)
        assert128BitHash("b".repeat(50), 7171538672710270498UL, 1631533914128298680UL)

        assert128BitHash("x".repeat(127), 9854593622914604095UL, 7658912557002357630UL)
        assert128BitHash("x".repeat(128), 7741783724626928635UL, 8757048405738817313UL)
        assert128BitHash("x".repeat(129), 12106413497735190516UL, 3639029327111625024UL)
        assert128BitHash("x".repeat(150), 11130683308639741584UL, 12527029032707456962UL)

        assert128BitHash("Hello World!".repeat(400), 14200190842850971771UL, 4403965390540562629UL)
    }

    companion object {
        val k0 = 0xc3a5c85c97cb3127UL
        val kSeed128 = Hash128Bits(1234567UL, k0)
    }

    private class ExpectedData(
        val _64Bit: ULong,
        val _128BitLow: ULong,
        val _128BitHigh: ULong,
        val _128BitLowSeed: ULong,
        val _128BitHighSeed: ULong
    ) {
        fun doTest(data: ByteArray, offset: Int, len: Int) {
            assertEquals(cityHash64(data, offset, len), _64Bit)
            assertEquals(cityHash128(data, offset, len), Hash128Bits(_128BitLow, _128BitHigh))
            assertEquals(cityHash128WithSeed(kSeed128, data, offset, len), Hash128Bits(_128BitLowSeed, _128BitHighSeed))
        }
    }

    fun testOriginal() {
        val testData = listOf(
            ExpectedData(0x9ae16a3b2f90404fUL, 0x3df09dfc64c09a2bUL, 0x3cb540c392e51e29UL, 0x6b56343feac0663UL, 0x5b7bc50fd8e8ad92UL),
            ExpectedData(0x541150e87f415e96UL, 0xc3cdc41e1df33513UL, 0x2c138ff2596d42f6UL, 0xf58e9082aed3055fUL, 0x162e192b2957163dUL),
            ExpectedData(0xf3786a4b25827c1UL, 0x3149ba1dac77270dUL, 0x70e2e076e30703cUL, 0x59bcc9659bc5296UL, 0x9ecbc8132ae2f1d7UL),
            ExpectedData(0xef923a7a1af78eabUL, 0x2193fb7620cbf23bUL, 0x8b6a8ff06cda8302UL, 0x1a44469afd3e091fUL, 0x8b0449376612506UL),
            ExpectedData(0x11df592596f41d88UL, 0x4d09e42f09cc3495UL, 0x666236631b9f253bUL, 0xd28b3763cd02b6a3UL, 0x43b249e57c4d0c1bUL),
            ExpectedData(0x831f448bdc5600b3UL, 0xdc07df53b949c6bUL, 0xd2b11b2081aeb002UL, 0xd212b02c1b13f772UL, 0xc0bed297b4be1912UL),
            ExpectedData(0x3eca803e70304894UL, 0xd183dcda5f73edfaUL, 0x3a93cbf40f30128cUL, 0x1a92544d0b41dbdaUL, 0xaec2c4bee81975e1UL),
            ExpectedData(0x1b5a063fb4c7f9f1UL, 0xb140a02ef5c97712UL, 0xb7d00ef065b51b33UL, 0x635121d532897d98UL, 0x532daf21b312a6d6UL),
            ExpectedData(0xa0f10149a0e538d6UL, 0x26b6689960ccf81dUL, 0x55f23b27bb9efd94UL, 0x3a17f6166dd765dbUL, 0xc891a8a62931e782UL),
            ExpectedData(0xfb8d9c70660b910bUL, 0x98ec31113e5e35d2UL, 0x5e4aeb853f1b9aa7UL, 0xbcf5c8fe4465b7c8UL, 0xb1ea3a8243996f15UL),
            ExpectedData(0x236827beae282a46UL, 0x71fec0f972248915UL, 0x2170ec2061f24574UL, 0x9eb346b6caa36e82UL, 0x2908f0fdbca48e73UL),
            ExpectedData(0xc385e435136ecf7cUL, 0xdf01a322c43a6200UL, 0x298b65a1714b5a7eUL, 0x933b83f0aedf23cUL, 0x157bcb44d63f765aUL),
            ExpectedData(0xe3f6828b6017086dUL, 0xd93251758985ee6cUL, 0x32a9e9f82ba2a932UL, 0x3822aacaa95f3329UL, 0xdb349b2f90a490d8UL),
            ExpectedData(0x851fff285561dca0UL, 0x77a4ccacd131d9eeUL, 0xe1d08eeb2f0e29aaUL, 0x70b9e3051383fa45UL, 0x582d0120425cabaUL),
            ExpectedData(0x61152a63595a96d9UL, 0xa154296d11362d06UL, 0xd0f0bf1f1cb02fc1UL, 0xccb87e09309f90d1UL, 0xb24a8e4881911101UL),
            ExpectedData(0x44473e03be306c88UL, 0x3bab18b164396783UL, 0x47e385ff9d4c06fUL, 0x18062081bf558dfUL, 0x63416eb68f104a36UL),
            ExpectedData(0x3ead5f21d344056UL, 0xac059617f5906673UL, 0x94d50d3dcd3069a7UL, 0x2b26c3b92dea0f0UL, 0x99b7374cc78fc3fbUL),
            ExpectedData(0x6abbfde37ee03b5bUL, 0xa4375590b8ae7c82UL, 0x168fd42f9ecae4ffUL, 0x23bbde43de2cb214UL, 0xa8c333112a243c8cUL),
            ExpectedData(0x943e7ed63b3c080UL, 0x6b54fc38d6a84108UL, 0x32f4212a47a4665UL, 0x6b5a9a8f64ee1da6UL, 0x9f74e86c6da69421UL),
            ExpectedData(0xd72ce05171ef8a1aUL, 0xf86af0b40dcce7bUL, 0x8d3c15d613394d3cUL, 0x491e400491cd4eceUL, 0x7c19d3530ea3547fUL),
            ExpectedData(0x4182832b52d63735UL, 0x7ebc034235bc122fUL, 0xd9a7783d4edd8049UL, 0x5f8b04a15ae42361UL, 0xfc193363336453ddUL),
            ExpectedData(0xd6cdae892584a2cbUL, 0x9e4ea5a4941e097dUL, 0x547e048d5a9daabaUL, 0xeb6ecbb0b831d185UL, 0xe0168df5fad0c670UL),
            ExpectedData(0x5c8e90bc267c5ee4UL, 0xce2744521944f14cUL, 0x104f8032f99dc152UL, 0x4e7f425bfac67ca7UL, 0x9461b911a1c6d589UL),
            ExpectedData(0xbbd7f30ac310a6f3UL, 0x4ee107042e512374UL, 0x1e2c8c0d16097e13UL, 0x210c7500995aa0e6UL, 0x6c13190557106457UL),
            ExpectedData(0x36a097aa49519d97UL, 0x6ee1f817ce0b7aeeUL, 0xe9dcb3507f0596caUL, 0x6bc63c666b5100e2UL, 0xe0b056f1821752afUL),
            ExpectedData(0xdc78cb032c49217UL, 0xd367ff54952a958UL, 0xcdad930657371147UL, 0xaa24dc2a9573d5feUL, 0xeb136daa89da5110UL),
            ExpectedData(0x441593e0da922dfeUL, 0x50d8a70e7a8d8f56UL, 0x256d150ae75dab76UL, 0xe81f4c4a1989036aUL, 0xd0f8db365f9d7e00UL),
            ExpectedData(0x2ba3883d71cc2133UL, 0xa90f761e8db1543aUL, 0xc339e23c09703cd8UL, 0xf0c6624c4b098fd3UL, 0x1bae2053e41fa4d9UL),
            ExpectedData(0xf2b6d2adf8423600UL, 0x23dacb811652ad4fUL, 0xc982da480e0d4c7dUL, 0x3a9c8ed5a399d0a9UL, 0x951b8d084691d4e4UL),
            ExpectedData(0x38fffe7f3680d63cUL, 0xc801faaa0a2e331fUL, 0x491dbc58279c7f88UL, 0x9c0178848321c97aUL, 0x9d934f814f4d6a3cUL),
            ExpectedData(0xb7477bf0b9ce37c6UL, 0x68dd76db9d64eca7UL, 0x36297682b64b67UL, 0x42b192d71f414b7aUL, 0x79692cef44fa0206UL),
            ExpectedData(0x55bdb0e71e3edebdUL, 0xb2e25964cd409117UL, 0xa010599d6287c412UL, 0xfa5d6461e768dda2UL, 0xcb3ce74e8ec4f906UL),
            ExpectedData(0x782fa1b08b475e7UL, 0x9a8c431f500ef06eUL, 0xd848581a580b6c12UL, 0xfecfe11e13a2bdb4UL, 0x6c4fa0273d7db08cUL),
            ExpectedData(0xc5dc19b876d37a80UL, 0x7870765b470b2c5dUL, 0x78a9103ff960d82UL, 0x7bb50ffc9fac74b3UL, 0x477e70ab2b347db2UL),
            ExpectedData(0x5e1141711d2d6706UL, 0xea349dbc16c2e441UL, 0x38a7455b6a877547UL, 0x5f97b9750e365411UL, 0xe8cde7f93af49a3UL),
            ExpectedData(0x782edf6da001234fUL, 0x5d9dde77353b1a6dUL, 0x11f58c54581fa8b1UL, 0xda90fa7c28c37478UL, 0x5e9a2eafc670a88aUL),
            ExpectedData(0xd26285842ff04d44UL, 0xbf41e5376b9f0eecUL, 0x2252d21eb7e1c0e9UL, 0xf4b70a971855e732UL, 0x40c7695aa3662afdUL),
            ExpectedData(0xc6ab830865a6bae6UL, 0xa1924cbf0b5f9222UL, 0x7f4872369c2b4258UL, 0xcd6da30530f3ea89UL, 0xb7f8b9a704e6cea1UL),
            ExpectedData(0x44b3a1929232892UL, 0xf7dbc8433c89b274UL, 0x2f5f70581c9b7d32UL, 0x39bf5e5fec82dccaUL, 0x8ade56388901a619UL),
            ExpectedData(0x4b603d7932a8de4fUL, 0x8ffe870ef4adc087UL, 0x65bea2be41f55b54UL, 0x82f3503f636aef1UL, 0x5f78a282378b6bb0UL),
            ExpectedData(0x4ec0b54cf1566affUL, 0x3df9b04434771542UL, 0xfeddce785ccb661fUL, 0xa644aff716928297UL, 0xdd46aee73824b4edUL),
            ExpectedData(0xed8b7a4b34954ff7UL, 0x7d2c38a926dc1b88UL, 0x5245b9eb4cd6791dUL, 0xfb53ab03b9ad0855UL, 0x3664026c8fc669d7UL),
            ExpectedData(0x5d28b43694176c26UL, 0x864b1b28ec16ea86UL, 0x6a78a5a4039ec2b9UL, 0x8e959533e35a766UL, 0x347b7c22b75ae65fUL),
            ExpectedData(0x6a1ef3639e1d202eUL, 0x2e8c49d7c7aaa527UL, 0x5e2328fc8701db7cUL, 0x89ef1afca81f7de8UL, 0xb1857db11985d296UL),
            ExpectedData(0x159f4d9e0307b111UL, 0x3b69edadf357432bUL, 0x3a2e311c121e6bf2UL, 0x380fad1e288d57e5UL, 0xbf7c7e8ef0e3b83aUL),
            ExpectedData(0xcc0a840725a7e25bUL, 0xcd7a46850b95e901UL, 0xc57f7d060dda246fUL, 0x6b9406ead64079bfUL, 0x11b28e20a573b7bdUL),
            ExpectedData(0xa2b27ee22f63c3f1UL, 0x8c1df927a930af59UL, 0xa462f4423c9e384eUL, 0x236542255b2ad8d9UL, 0x595d201a2c19d5bcUL),
            ExpectedData(0xd8f2f234899bcab3UL, 0x9498fefb890287ceUL, 0xae68c2be5b1a69a6UL, 0x6189dfba34ed656cUL, 0x91658f95836e5206UL),
            ExpectedData(0x584f28543864844fUL, 0x7a0b6dbab9a14e69UL, 0xc6d0a9d6b0e31ac4UL, 0xa674d85812c7cf6UL, 0x63538c0351049940UL),
            ExpectedData(0xa94be46dd9aa41afUL, 0x843b58463c8df0aeUL, 0x74b258324e916045UL, 0xbdd7353230eb2b38UL, 0xfad31fced7abade5UL),
            ExpectedData(0x9a87bea227491d20UL, 0xcc76f429ea7a12bbUL, 0x5f30eaf2bb14870aUL, 0x434e824cb3e0cd11UL, 0x431a4d382e39d16eUL),
            ExpectedData(0x27688c24958d1a5cUL, 0x328063229db22884UL, 0x67e9c95f8ba96028UL, 0x7c6bf01c60436075UL, 0xfa55161e7d9030b2UL),
            ExpectedData(0x5d1d37790a1873adUL, 0xf72c26e624407e66UL, 0xa0eb541bdbc6d409UL, 0xc3f40a2f40b3b213UL, 0x6a784de68794492dUL),
            ExpectedData(0x1f03fd18b711eea9UL, 0x405f66cf8cae1a32UL, 0xd7261740d8f18ce6UL, 0xfea3af64a413d0b2UL, 0xd64d1810e83520feUL),
            ExpectedData(0xf0316f286cf527b6UL, 0xd4eccebe9393ee8aUL, 0x2eb7867c2318cc59UL, 0x1ce621fd700fe396UL, 0x686450d7a346878aUL),
            ExpectedData(0x297008bcb3e3401dUL, 0x7a61d8f552a53442UL, 0x821d1d8d8cfacf35UL, 0x7cc06361b86d0559UL, 0x119b617a8c2be199UL),
            ExpectedData(0x43c6252411ee3beUL, 0x2247a4b2058d1c50UL, 0x1b3fa184b1d7bcc0UL, 0xdeb85613995c06edUL, 0xcbe1d957485a3ccdUL),
            ExpectedData(0xce38a9a54fad6599UL, 0xe8b9ee96efa2d0eUL, 0x90122905c4ab5358UL, 0x84f80c832d71979cUL, 0x229310f3ffbbf4c6UL),
            ExpectedData(0x270a9305fef70cfUL, 0x2e091b85660f1298UL, 0xbfe37fae1cdd64c9UL, 0x8dddfbab930f6494UL, 0x2ccf4b08f5d417aUL),
            ExpectedData(0xe71be7c28e84d119UL, 0x7a9d77781ac53509UL, 0x4489c3ccfda3b39cUL, 0xfa722d4f243b4964UL, 0x25f15800bffdd122UL),
            ExpectedData(0xb5b58c24b53aaa19UL, 0x9deefbcfa4cab1f1UL, 0xb58f5943cd2492baUL, 0xa96dcc4d1f4782a7UL, 0x102b62a82309dde5UL),
            ExpectedData(0x44dd59bd301995cfUL, 0xcfc6d7adda35797UL, 0x14c7d1f32332cf03UL, 0x2d553ffbff3be99dUL, 0xc91c4ee0cb563182UL),
            ExpectedData(0xb4d4789eb6f2630bUL, 0xbce905900c1ec6eaUL, 0xc30f304f4045487dUL, 0xa5c550166b3a142bUL, 0x2f482b4e35327287UL),
            ExpectedData(0x12807833c463737cUL, 0x910b610de7a967bfUL, 0x801bc862120f6bf5UL, 0x9653efeed5897681UL, 0xf5367ff83e9ebbb3UL),
            ExpectedData(0xe88419922b87176fUL, 0xd1d44fe99451ef72UL, 0xec951ba8e51e3545UL, 0xc0ca86b360746e96UL, 0xaa679cc066a8040bUL),
            ExpectedData(0x105191e0ec8f7f60UL, 0xd3e86ac4f5eccfa4UL, 0xe5399df2b106ca1UL, 0x814aadfacd217f1dUL, 0x2754e3def1c405a9UL),
            ExpectedData(0xa5b88bf7399a9f07UL, 0x69afbc800606d0fbUL, 0x6104b97a9db12df7UL, 0xfcc09198bb90bf9fUL, 0xc5e077e41a65ba91UL),
            ExpectedData(0xd08c3f5747d84f50UL, 0x909ae019d761d019UL, 0x368bf4aab1b86ef9UL, 0x308bd616d5460239UL, 0x4fd33269f76783eaUL),
            ExpectedData(0x2f72d12a40044b4bUL, 0xef79f28d874b9e2dUL, 0xb512089e8e63b76cUL, 0x24dc06833bf193a9UL, 0x3c23308ba8e99d7eUL),
            ExpectedData(0xaa1f61fdc5c2e11eUL, 0x8184bab36bb79df0UL, 0xc81929ce8655b940UL, 0x301b11bf8a4d8ce8UL, 0x73126fd45ab75de9UL),
            ExpectedData(0x9489b36fe2246244UL, 0xbc61414f9802ecafUL, 0x8edd1e7a50562924UL, 0x48f4ab74a35e95f2UL, 0xcc1afcfd99a180e7UL),
            ExpectedData(0x358d7c0476a044cdUL, 0xd45e44c263e95c38UL, 0xdf61db53923ae3b1UL, 0xf2bc948cc4fc027cUL, 0x8a8000c6066772a3UL),
            ExpectedData(0xb0c48df14275265aUL, 0x30e888af70df1e56UL, 0x4bee54bd47274f69UL, 0x178b4059e1a0afe5UL, 0x6e2c96b7f58e5178UL),
            ExpectedData(0xdaa70bb300956588UL, 0x8b1d7bb4903c105fUL, 0xcfb1c322b73891d4UL, 0x5f3b792b22f07297UL, 0xfd64061f8be86811UL),
            ExpectedData(0x4ec97a20b6c4c7c2UL, 0x852c9499156a8f3UL, 0x3a180a6abfb79016UL, 0x9fc3c4764037c3c9UL, 0x2890c42fc0d972cfUL),
            ExpectedData(0x5c3323628435a2e8UL, 0x939f31de14dcdc7bUL, 0xa68fdf4379df068UL, 0xf169e1f0b835279dUL, 0x7498e432f9619b27UL),
            ExpectedData(0xc1ef26bea260abdbUL, 0x11b87fb1b900cc39UL, 0xe33e59b90dd815b1UL, 0xaa6cb5c4bafae741UL, 0x739699951ca8c713UL),
            ExpectedData(0x6be7381b115d653aUL, 0xa64760e4041447d0UL, 0xe3eac49f3e0c5109UL, 0xdd86c4d4cb6258e2UL, 0xefa9857afd046c7fUL),
            ExpectedData(0xae3eece1711b2105UL, 0x501f3e9b18861e44UL, 0x465201170074e7d8UL, 0x96d5c91970f2cb12UL, 0x40fd28c43506c95dUL),
            ExpectedData(0x376c28588b8fb389UL, 0x154dd79fd2f984b4UL, 0xf11171775622c1c3UL, 0x1fbe30982e78e6f0UL, 0xa460a15dcf327e44UL),
            ExpectedData(0x58d943503bb6748fUL, 0xb7e164979d5ccfc1UL, 0x12cb4230d26bf286UL, 0xf1bf910d44bd84cbUL, 0xb32c24c6a40272UL),
            ExpectedData(0xdfff5989f5cfd9a1UL, 0x3ff6c8ac7c36b63aUL, 0x48bc8831d849e326UL, 0x30b078e76b0214e2UL, 0x42954e6ad721b920UL),
            ExpectedData(0x7fb19eb1a496e8f5UL, 0x1a57313a32f22ddeUL, 0x30af46e49850bf8bUL, 0xaa0fe8d12f808f83UL, 0x443e31d70873bb6bUL),
            ExpectedData(0x5dba5b0dadccdbaaUL, 0xe9029e6364286587UL, 0xae69f49ecb46726cUL, 0x18e002679217c405UL, 0xbd6d66e85332ae9fUL),
            ExpectedData(0x688bef4b135a6829UL, 0x3d8c90e27aa2e147UL, 0x2ec937ce0aa236b4UL, 0x89b563996d3a0b78UL, 0x39b02413b23c3f08UL),
            ExpectedData(0xd8323be05433a412UL, 0x4d50c7537562033fUL, 0x57dc7625b61dfe89UL, 0x9723a9f4c08ad93aUL, 0x5309596f48ab456bUL),
            ExpectedData(0x3b5404278a55a7fcUL, 0x45504801e0e6066bUL, 0x86e6c6d6152a3d04UL, 0x4f3db1c53eca2952UL, 0xd24d69b3e9ef10f3UL),
            ExpectedData(0x2a96a3f96c5e9bbcUL, 0xf13bc2d9c2fe222eUL, 0xbe4ccec9a6cdccfdUL, 0x37b2cbdd973a3ac9UL, 0x7b3223cd9c9497beUL),
            ExpectedData(0x22bebfdcc26d18ffUL, 0x3752b423073b119aUL, 0x377dc5eb7c662bdbUL, 0x2b9f07f93a6c25b9UL, 0x96f24ede2bdc0718UL),
            ExpectedData(0x627a2249ec6bbcc2UL, 0xebdbb918eb6d837fUL, 0x8fb5f218dd84147cUL, 0xc77dd1f881df2c54UL, 0x62eac298ec226dc3UL),
            ExpectedData(0x3abaf1667ba2f3e0UL, 0xf1b9b413df9d79edUL, 0xa7621b6fd02db503UL, 0xd92f7ba9928a4ffeUL, 0x53f56babdcae96a6UL),
            ExpectedData(0x3931ac68c5f1b2c9UL, 0xa53a6b64b1ac85c9UL, 0xd50e7f86ee1b832bUL, 0x7bab08fdd26ba0a4UL, 0x7587743c18fe2475UL),
            ExpectedData(0xb98fb0606f416754UL, 0xdbfaae9642b3205aUL, 0xf676a1339402bcb9UL, 0xf4f12a5b1ac11f29UL, 0x7db8bad81249dee4UL),
            ExpectedData(0x7f7729a33e58fcc4UL, 0x47418a71800334a0UL, 0xd10395d8fc64d8a4UL, 0x8257a30062cb66fUL, 0x6786f9b2dc1ff18aUL),
            ExpectedData(0x42a0aa9ce82848b3UL, 0xcaa33cf9b4f6619cUL, 0xb2c8648ad49c209fUL, 0x9e89ece0712db1c0UL, 0x101d8274a711a54bUL),
            ExpectedData(0x6b2c6d38408a4889UL, 0x941f5023c0c943f9UL, 0xdfdeb9564fd66f24UL, 0x2140cec706b9d406UL, 0x7b22429b131e9c72UL),
            ExpectedData(0x930380a3741e862aUL, 0x7e7f61684080106UL, 0x837ace9794582976UL, 0x5ac8ca76a357eb1bUL, 0x32b58308625661fbUL),
            ExpectedData(0x94808b5d2aa25f9aUL, 0x272d8dd74f3006ccUL, 0xec6c2ad1ec03f554UL, 0x4ad276b249a5d5ddUL, 0x549a22a17c0cde12UL),
            ExpectedData(0xb31abb08ae6e3d38UL, 0x7b2271a7a3248e22UL, 0x3b4f700e5a0ba523UL, 0x8ebc520c227206feUL, 0xda3f861490f5d291UL),
            ExpectedData(0xdccb5534a893ea1aUL, 0x3f1229f4d0fd96fbUL, 0x33130aa5fa9d43f2UL, 0xe42693d5b34e63abUL, 0x2f4ef2be67f62104UL),
            ExpectedData(0x6369163565814de6UL, 0x7d3e82d5ba29a90dUL, 0xd5983cc93a9d126aUL, 0x37e9dfd950e7b692UL, 0x80673be6a7888b87UL),
            ExpectedData(0xedee4ff253d9f9b3UL, 0x1f3dcdfa513512d6UL, 0x4dc7ec07283117e4UL, 0x4438bae88ae28bf9UL, 0xaa7eae72c9244a0dUL),
            ExpectedData(0x941993df6e633214UL, 0xb3b782ad308f21edUL, 0x4f2676485041dee0UL, 0xbfe279aed5cb4bc8UL, 0x2a62508a467a22ffUL),
            ExpectedData(0x859838293f64cd4cUL, 0x44d68afda9568f08UL, 0x478568ed51ca1d65UL, 0x679c204ad3d9e766UL, 0xb28e788878488dc1UL),
            ExpectedData(0xc19b5648e0d9f555UL, 0xc3314e362764ddb8UL, 0x6481c084ee9ec6b5UL, 0xede23fb9a251771UL, 0xbd617f2643324590UL),
            ExpectedData(0xf963b63b9006c248UL, 0x2c6aa706129cc54cUL, 0x17a706f59a49f086UL, 0xc7c1eec455217145UL, 0x6adfdc6e07602d42UL),
            ExpectedData(0x6a8aa0852a8c1f3bUL, 0xfc3e3c322cd5d89bUL, 0xb7e3911dc2bd4ebbUL, 0xfcd6da5e5fae833aUL, 0x51ed3c41f87f9118UL),
            ExpectedData(0x740428b4d45e5fb8UL, 0x914f1ea2fdcebf5cUL, 0x9566453c07cd0601UL, 0x9841bf66d0462cdUL, 0x79140c1c18536aebUL),
            ExpectedData(0x658b883b3a872b86UL, 0x99468a917986162bUL, 0x7b31434aac6e0af0UL, 0xf6915c1562c7d82fUL, 0xe4071d82a6dd71dbUL),
            ExpectedData(0x6df0a977da5d27d4UL, 0x8799e4740e573c50UL, 0x9e739b52d0f341e8UL, 0xcdfd34ba7d7b03ebUL, 0x5061812ce6c88499UL),
            ExpectedData(0xa900275464ae07efUL, 0x8063d80ab26f3d6dUL, 0x4177b4b9b4f0393fUL, 0x6de42ba8672b9640UL, 0xd0bccdb72c51c18UL),
            ExpectedData(0x810bc8aa0c40bcb0UL, 0x52c44837aa6dfc77UL, 0x15d8d8fccdd6dc5bUL, 0x345b793ccfa93055UL, 0x932160fe802ca975UL),
            ExpectedData(0x22036327deb59ed7UL, 0xc791b313aba3f258UL, 0x443c7757a4727beeUL, 0xe30e4b2372171bdfUL, 0xf3db986c4156f3cbUL),
            ExpectedData(0x7d14dfa9772b00c8UL, 0xbc241579d8348401UL, 0x16dc832804d728f0UL, 0xe9cc71ae64e3f09eUL, 0xbef634bc978bac31UL),
            ExpectedData(0x2d777cddb912675dUL, 0x4283001239888836UL, 0xf44ca39a6f79db89UL, 0xed186122d71bcc9fUL, 0x8620017ab5f3ba3bUL),
            ExpectedData(0xf2ec98824e8aa613UL, 0x374dd4288e0b72e5UL, 0xff8916db706c0df4UL, 0xcb1a9e85de5e4b8dUL, 0xd4d12afb67a27659UL),
            ExpectedData(0x5e763988e21f487fUL, 0x9136456740119815UL, 0x4d8ff7733b27eb83UL, 0xea3040bc0c717ef8UL, 0x7617ab400dfadbcUL),
            ExpectedData(0x48949dc327bb96adUL, 0x14cf7f02dab0eee8UL, 0x6d01750605e89445UL, 0x4f1cf4006e613b78UL, 0x57c40c4db32bec3bUL),
            ExpectedData(0xb7c4209fb24a85c5UL, 0x570d62758ddf6397UL, 0x5e0204fb68a7b800UL, 0x4383a9236f8b5a2bUL, 0x7bc1a64641d803a4UL),
            ExpectedData(0x9c9e5be0943d4b05UL, 0xc738a77a9a55f0e2UL, 0x705221addedd81dfUL, 0xfd9bd8d397abcfa3UL, 0x8ccf0004aa86b795UL),
            ExpectedData(0x3898bca4dfd6638dUL, 0x9b82567ab6560796UL, 0x891b69462b41c224UL, 0x8eccc7e4f3af3b51UL, 0x381e54c3c8f1c7d0UL),
            ExpectedData(0x5b5d2557400e68e7UL, 0x3c13e894365dc6c2UL, 0x26fc7bbcda3f0efUL, 0xdbb71106cdbfea36UL, 0x785239a742c6d26dUL),
            ExpectedData(0xa927ed8b2bf09bb6UL, 0x6e65ec14a8fb565UL, 0x34bff6f2ee5a7f79UL, 0x2e329a5be2c011bUL, 0x73161c93331b14f9UL),
            ExpectedData(0x8d25746414aedf28UL, 0x379f76458a3c8957UL, 0x79dd080f9843af77UL, 0xc46f0a7847f60c1dUL, 0xaf1579c5797703ccUL),
            ExpectedData(0xb5bbdb73458712f2UL, 0x1e6f0910c3d25bd8UL, 0xad9e250862102467UL, 0x1c842a07abab30cdUL, 0xcd8124176bac01acUL),
            ExpectedData(0x3d32a26e3ab9d254UL, 0xb1cf09b0184a4834UL, 0x5c03db48eb6cc159UL, 0xf18c7fcf34d1df47UL, 0xdfb043419ecf1fa9UL),
            ExpectedData(0x9371d3c35fa5e9a5UL, 0xceaf1a0d15234f15UL, 0x1450a54e45ba9b9UL, 0x65e9c1fd885aa932UL, 0x354d4bc034ba8cbeUL),
            ExpectedData(0xcbaa3cb8f64f54e0UL, 0x85b8e53f22e19507UL, 0xbb57137739ca486bUL, 0xc77f131cca38f761UL, 0xc56ac3cf275be121UL),
            ExpectedData(0xb2e23e8116c2ba9fUL, 0xadc52dddb76f6e5eUL, 0x4aad4e925a962b68UL, 0x204b79b7f7168e64UL, 0xdf29ed6671c36952UL),
            ExpectedData(0x8aa77f52d7868eb9UL, 0xce030d15b5fe2f4UL, 0x86b4a7a0780c2431UL, 0xee070a9ae5b51db7UL, 0xedc293d9595be5d8UL),
            ExpectedData(0x858fea922c7fe0c3UL, 0x64fd1bc011e5bab7UL, 0x5c9e858728015568UL, 0x97ac42c2b00b29b1UL, 0x7f89caf08c109aeeUL),
            ExpectedData(0x46ef25fdec8392b1UL, 0xfdfa836b41dcef62UL, 0x2f8db8030e847e1bUL, 0x5ba0a49ac4f9b0f8UL, 0xdae897ed3e3fce44UL),
            ExpectedData(0x8d078f726b2df464UL, 0x7d222caae025158aUL, 0xcc028d5fd40241b9UL, 0xdd42515b639e6f97UL, 0xe08e86531a58f87fUL),
            ExpectedData(0x35ea86e6960ca950UL, 0x80395e48739e1a67UL, 0x74a67d8f7f43c3d7UL, 0xdd2bdd1d62246c6eUL, 0xa1f44298ba80acf6UL),
            ExpectedData(0x8aee9edbc15dd011UL, 0x133b299a939745c5UL, 0x796e2aac053f52b3UL, 0xe8d9fe1521a4a222UL, 0x819a8863e5d1c290UL),
            ExpectedData(0xc3e142ba98432ddaUL, 0xfd1a9ba5e71b08a2UL, 0x7ac0dc2ed7778533UL, 0xb543161ff177188aUL, 0x492fc08a6186f3f4UL),
            ExpectedData(0x123ba6b99c8cd8dbUL, 0x938f5bbab544d3d6UL, 0xd2a95f9f2d376d73UL, 0x68b2f16149e81aa3UL, 0xad7e32f82d86c79dUL),
            ExpectedData(0xba87acef79d14f53UL, 0xeea5f5a9f74af591UL, 0x578710bcc36fbea2UL, 0x7a8393432188931dUL, 0x705cfc5ec7cc172UL),
            ExpectedData(0xbcd3957d5717dc3UL, 0x2b826f1a2c08c289UL, 0xda50f56863b55e74UL, 0xb18712f6b3eed83bUL, 0xbdc7cc05ab4c685fUL),
            ExpectedData(0x61442ff55609168eUL, 0xeffc2663cffc777fUL, 0x93214f8f463afbedUL, 0xa156ef06066f4e4eUL, 0xa407b6ed8769d51eUL),
            ExpectedData(0xdbe4b1b2d174757fUL, 0x5a4fc2728a9bb671UL, 0xebb971522ec38759UL, 0x1a5a093e6cf1f72bUL, 0x729b057fe784f504UL),
            ExpectedData(0x531e8e77b363161cUL, 0xe777b1fd580582f2UL, 0x7b880f58da112699UL, 0x562c6b189a6333f4UL, 0x139d64f88a611d4UL),
            ExpectedData(0xf71e9c926d711e2bUL, 0xdd16cd0fbc08393UL, 0x29a414a5d8c58962UL, 0x72793d8d1022b5b2UL, 0x2e8e69cf7cbffdf0UL),
            ExpectedData(0xcb20ac28f52df368UL, 0x4260e8c254e9924bUL, 0xf197a6eb4591572dUL, 0x8e867ff0fb7ab27cUL, 0xf95502fb503efaf3UL),
            ExpectedData(0xe4a794b4acb94b55UL, 0x4890a83ee435bc8bUL, 0xd8c1c00fceb00914UL, 0x9e7111ba234f900fUL, 0xeb8dbab364d8b604UL),
            ExpectedData(0xcb942e91443e7208UL, 0x8ba0fdd2ffc8b239UL, 0xf413b366c1ffe02fUL, 0xc05b2717c59a8a28UL, 0x981188eab4fcc8fbUL),
            ExpectedData(0xecca7563c203f7baUL, 0xcf1edbfe7330e94eUL, 0x881945906bcb3cc6UL, 0x4acf0293244855daUL, 0x65ae042c1c2a28c2UL),
            ExpectedData(0x1652cb940177c8b5UL, 0xf6521b912b368ae6UL, 0xa9fe4eff81d03e73UL, 0xd6f623629f80d1a3UL, 0x2b9604f32cb7dc34UL),
            ExpectedData(0x31fed0fc04c13ce8UL, 0x6b5ffc1f54fecb29UL, 0xa8e8e7ad5b9a21d9UL, 0xc4d5a32cd6aac22dUL, 0xd7e274ad22d4a79aUL),
            ExpectedData(0xe7b668947590b9b3UL, 0x381ee1b7ea534f4eUL, 0xda3759828e3de429UL, 0x3e015d76729f9955UL, 0xcbbec51a6485fbdeUL),
            ExpectedData(0x1de2119923e8ef3cUL, 0x4cc8ed3ada5f0f2UL, 0x4a496b77c1f1c04eUL, 0x9085b0a862084201UL, 0xa1894bde9e3dee21UL),
            ExpectedData(0x1269df1e69e14fa7UL, 0xe5d0549802d15008UL, 0x424c134ecd0db834UL, 0x6fc44fd91be15c6cUL, 0xa1a5ef95d50e537dUL),
            ExpectedData(0x820826d7aba567ffUL, 0xaa0d74d4a98db89bUL, 0x36fd486d07c56e1dUL, 0xd0ad23cbb6660d8aUL, 0x1264a84665b35e19UL),
            ExpectedData(0xffe0547e4923cef9UL, 0x28ac84ca70958f7eUL, 0xd8ae575a68faa731UL, 0x2aaaee9b9dcffd4cUL, 0x6c7faab5c285c6daUL),
            ExpectedData(0x72da8d1b11d8bc8bUL, 0x43505ed133be672aUL, 0xe8f2f9d973c2774eUL, 0x677b9b9c7cad6d97UL, 0x4e1f5d56ef17b906UL),
            ExpectedData(0xd62ab4e3f88fc797UL, 0x4344a1a0134afe2UL, 0xff5c17f02b62341dUL, 0x3214c6a587ce4644UL, 0xa905e7ed0629d05cUL),
            ExpectedData(0xd0f06c28c7b36823UL, 0x489b697fe30aa65fUL, 0x4da0fb621fdc7817UL, 0xdc43583b82c58107UL, 0x4b0261debdec3cd6UL),
            ExpectedData(0x99b7042460d72ec6UL, 0xc043e67e6fc64118UL, 0xff0abfe926d844d3UL, 0xf2a9fe5db2e910feUL, 0xce352cdc84a964ddUL),
            ExpectedData(0x4f4dfcfc0ec2bae5UL, 0x334c5a25b5903a8cUL, 0x4c94fef443122128UL, 0x743e7d8454655c40UL, 0x1ab1e6d1452ae2cdUL),
            ExpectedData(0xfe86bf9d4422b9aeUL, 0x8bde625a10a8c50dUL, 0xeb8271ded1f79a0bUL, 0x14dc6844f0de7a3cUL, 0xf85b2f9541e7e6daUL),
            ExpectedData(0xa90d81060932dbb0UL, 0xdd52fc14c8dd3143UL, 0x1bc7508516e40628UL, 0x3059730266ade626UL, 0xffa526822f391c2UL),
            ExpectedData(0x17938a1b0e7f5952UL, 0xc1336b92fef91bf6UL, 0x80332a3945f33fa9UL, 0xa0f68b86f726ff92UL, 0xa3db5282cf5f4c0bUL),
            ExpectedData(0xde9e0cb0e16f6e6dUL, 0x497cb912b670f3bUL, 0xd963a3f02ff4a5b6UL, 0x4fccefae11b50391UL, 0x42ba47db3f7672fUL),
            ExpectedData(0x6d4b876d9b146d1aUL, 0x2fe9fabdbe7fdd4UL, 0x755db249a2d81a69UL, 0xf27929f360446d71UL, 0x79a1bf957c0c1b92UL),
            ExpectedData(0xe698fa3f54e6ea22UL, 0xd53fb7e3c93a9e4UL, 0x737ae71b051bf108UL, 0x7ac71feb84c2df42UL, 0x3d8075cd293a15b4UL),
            ExpectedData(0x7bc0deed4fb349f7UL, 0xcf7d7f25bd70cd2cUL, 0x9464ed9baeb41b4fUL, 0xb9064f5c3cb11b71UL, 0x237e39229b012b20UL),
            ExpectedData(0xdb4b15e88533f622UL, 0x9040e5b936b8661bUL, 0x276e08fa53ac27fdUL, 0x8c944d39c2bdd2ccUL, 0xe2514c9802a5743cUL),
            ExpectedData(0x922834735e86ecb2UL, 0x8431b1bfd0a2379cUL, 0x90383913aea283f9UL, 0xa6163831eb4924d2UL, 0x5f3921b4f9084aeeUL),
            ExpectedData(0x30f1d72c812f1eb8UL, 0xc54677a80367125eUL, 0x3204fbdba462e606UL, 0x8563278afc9eae69UL, 0x262147dd4bf7e566UL),
            ExpectedData(0x168884267f3817e9UL, 0x9598f6ab0683fcc2UL, 0x1c805abf7b80e1eeUL, 0xdec9ac42ee0d0f32UL, 0x8cd72e3912d24663UL),
            ExpectedData(0x82e78596ee3e56a7UL, 0x6ba372f4b7ab268bUL, 0x8c3237cf1fe243dfUL, 0x3833fc51012903dfUL, 0x8e31310108c5683fUL),
            ExpectedData(0xaa2d6cf22e3cc252UL, 0x9a62af3dbba140daUL, 0x27857ea044e9dfc1UL, 0x33abce9da2272647UL, 0xb22a7993aaf32556UL),
            ExpectedData(0x7bf5ffd7f69385c7UL, 0x82065c62e6582188UL, 0x8ef787fd356f5e43UL, 0x2922e53e36e17dfaUL, 0x9805f223d385010bUL),
            ExpectedData(0xe89c8ff9f9c6e34bUL, 0x22f2aa3df2221ccUL, 0xf66fea90f5d62174UL, 0xb75defaeaa1dd2a7UL, 0x9b994cd9a7214fd5UL),
            ExpectedData(0xa18fbcdccd11e1f4UL, 0x229b79ab69ae97dUL, 0xa87aabc2ec26e582UL, 0xbe2b053721eb26d2UL, 0x10febd7f0c3d6fcbUL),
            ExpectedData(0x2d54f40cc4088b17UL, 0xd332cdb073d8dc46UL, 0x272c56466868cb46UL, 0x7e7fcbe35ca6c3f3UL, 0xee8f51e5a70399d4UL),
            ExpectedData(0x69276946cb4e87c7UL, 0x702e2afc7f5a1825UL, 0x8c49b11ea8151fdcUL, 0xcaf3fef61f5a86faUL, 0xef0b2ee8649d7272UL),
            ExpectedData(0x668174a3f443df1dUL, 0xa590b202a7a5807bUL, 0x968d2593f7ccb54eUL, 0x9dd8d669e3e95decUL, 0xee0cc5dd58b6e93aUL),
            ExpectedData(0x5e29be847bd5046UL, 0x7432d63888e0c306UL, 0x74bbceeed479cb71UL, 0x6471586599575fdfUL, 0x6a859ad23365cba2UL),
            ExpectedData(0xcd0d79f2164da014UL, 0x69db23875cb0b715UL, 0xada8dd91504ae37fUL, 0x46bf18dbf045ed6aUL, 0xe1b5f67b0645ab63UL),
            ExpectedData(0xe0e6fc0b1628af1dUL, 0xc4af7faf883033aaUL, 0x9bd296c4e9453cacUL, 0xca45426c1f7e33f9UL, 0xa6bbdcf7074d40c5UL),
            ExpectedData(0x2058927664adfd93UL, 0x42e34cf3d53c7876UL, 0x9cddbb26424dc5eUL, 0x64f6340a6d8eddadUL, 0x2196e488eb2a3a4bUL),
            ExpectedData(0xdc107285fd8e1af7UL, 0xbcc7a81ed5432429UL, 0xb6d7bdc6ad2e81f1UL, 0x93605ec471aa37dbUL, 0xa2a73f8a85a8e397UL),
            ExpectedData(0xfbba1afe2e3280f1UL, 0x6226a32e25099848UL, 0xea895661ecf53004UL, 0x4d7e0158db2228b9UL, 0xe5a7d82922f69842UL),
            ExpectedData(0xbfa10785ddc1011bUL, 0xca6552a0dfb82c73UL, 0xb024cdf09e34ba07UL, 0x66cd8c5a95d7393bUL, 0xe3939acf790d4a74UL),
            ExpectedData(0x534cc35f0ee1eb4eUL, 0xf14ef7f47d8a57a3UL, 0x80d1f86f2e061d7cUL, 0x401d6c2f151b5a62UL, 0xe988460224108944UL),
            ExpectedData(0x7ca6e3933995dacUL, 0xc8389799445480dbUL, 0x5389f5df8aacd50dUL, 0xd136581f22fab5fUL, 0xc2f31f85991da417UL),
            ExpectedData(0xf0d6044f6efd7598UL, 0x70bd1968996bffc2UL, 0x4c613de5d8ab32acUL, 0xfe1f4f97206f79d8UL, 0xac0434f2c4e213a9UL),
            ExpectedData(0x3d69e52049879d61UL, 0x8eeb177a86053c11UL, 0xe390122c345f34a2UL, 0x1e30e47afbaaf8d6UL, 0x7b892f68e5f91732UL),
            ExpectedData(0x79da242a16acae31UL, 0x27233b28b5b11e9bUL, 0xc7dfe8988a942700UL, 0x570ed11c4abad984UL, 0x4b4c04632f48311aUL),
            ExpectedData(0x461c82656a74fb57UL, 0x49fa3070bc7b06d0UL, 0xf12ed446bd0c0539UL, 0x6d43ac5d1dd4b240UL, 0x7609524fe90bec93UL),
            ExpectedData(0x53c1a66d0b13003UL, 0x57466046cf6896edUL, 0x8ac37e0e8b25b0c6UL, 0x3e6074b52ad3cf18UL, 0xaa491ce7b45db297UL),
            ExpectedData(0xd3a2efec0f047e9UL, 0xc2dcc9758c910171UL, 0xcb5cddaeff4ddb40UL, 0x5d7cc5869baefef1UL, 0x9644c5853af9cfebUL),
            ExpectedData(0x43c64d7484f7f9b2UL, 0x3ee84d3d5b4ca00bUL, 0x5cbc6d701894c3f9UL, 0xd9e946f5ae1ca95UL, 0x24ca06e67f0b1833UL),
            ExpectedData(0xa7dec6ad81cf7fa1UL, 0x6b11c5073687208UL, 0x7e0a57de0d453f3UL, 0xe48c267d4f646867UL, 0x2168e9136375f9cbUL),
            ExpectedData(0x5408a1df99d4affUL, 0x7da9e81d89fda7adUL, 0x274157cabe71440dUL, 0x2c22d9a480b331f7UL, 0xe835c8ac746472d5UL),
            ExpectedData(0xa8b27a6bcaeeed4bUL, 0xd45a938b79f54e8fUL, 0x366b219d6d133e48UL, 0x5b14be3c25c49405UL, 0xfdd791d48811a572UL),
            ExpectedData(0x9a952a8246fdc269UL, 0xc83d3c5f4e5f0320UL, 0x694e7adeb2bf32e5UL, 0x7ad09538a3da27f5UL, 0x2b5c18f934aa5303UL),
            ExpectedData(0xc930841d1d88684fUL, 0xbc271bc0df14d647UL, 0xb071100a9ff2edbbUL, 0x2b1a4c1cc31a119aUL, 0xb5d7caa1bd946cefUL),
            ExpectedData(0x94dc6971e3cf071aUL, 0x336c1b59a1fc19f6UL, 0xc173acaecc471305UL, 0xdb1267d24f3f3f36UL, 0xe9a5ee98627a6e78UL),
            ExpectedData(0x7fc98006e25cac9UL, 0x84064a6dcf916340UL, 0xfbf55a26790e0ebbUL, 0x2e7f84151c31a5c2UL, 0x9f7f6d76b950f9bfUL),
            ExpectedData(0xbd781c4454103f6UL, 0xe38e526cd3324364UL, 0x85f2b63a5b5e840aUL, 0x485d7cef5aaadd87UL, 0xd2b837a462f6db6dUL),
            ExpectedData(0xda60e6b14479f9dfUL, 0x16818ee9d38c6664UL, 0x5519fa9a1e35a329UL, 0xcbd0001e4b08ed8UL, 0x41a965e37a0c731bUL),
            ExpectedData(0x4ca56a348b6c4d3UL, 0x30278016830ddd43UL, 0xf046646d9012e074UL, 0xc62a5804f6e7c9daUL, 0x98d51f5830e2bc1eUL),
            ExpectedData(0xebd22d4b70946401UL, 0x7d2782b82bd494b6UL, 0x97159ba1c26b304bUL, 0x42b3b0fd431b2ac2UL, 0xfaa81f82691c830cUL),
            ExpectedData(0x3cc4693d6cbcb0cUL, 0x58c8aba7475e2d95UL, 0x3e2f291698c9427aUL, 0xe8710d19c9de9e41UL, 0x65dda22eb04cf953UL),
            ExpectedData(0x38908e43f7ba5ef0UL, 0xd1090893afaab8bcUL, 0x96c4fe6922772807UL, 0x4522426c2b4205ebUL, 0xefad99a1262e7e0dUL),
            ExpectedData(0x34983ccc6aa40205UL, 0xfc947167f69c0da5UL, 0xae79cfdb91b6f6c1UL, 0x7b251d04c26cbda3UL, 0x128a33a79060d25eUL),
            ExpectedData(0x86215c45dcac9905UL, 0xb7609c8e70386d66UL, 0x36e6ccc278d1636dUL, 0x2f873307c08e6a1cUL, 0x10f252a758505289UL),
            ExpectedData(0x420fc255c38db175UL, 0x4c10537443152f3dUL, 0x720451d3c895e25dUL, 0xaff60c4d11f513fdUL, 0x881e8d6d2d5fb953UL),
            ExpectedData(0x1d7a31f5bc8fe2f9UL, 0xf265edb0c1c411d7UL, 0x30e1e9ec5262b7e6UL, 0xc2c3ba061ce7957aUL, 0xd975f93b89a16409UL),
            ExpectedData(0x94129a84c376a26eUL, 0xe9369d2e9007e74bUL, 0xb1375915d1136052UL, 0x926c2021fe1d2351UL, 0x1d943addaaa2e7e6UL),
            ExpectedData(0x1d3a9809dab05c8dUL, 0x301d7a61c4b3dbcaUL, 0x861336c3f0552d61UL, 0x12c6db947471300fUL, 0xa679ef0ed761deb9UL),
            ExpectedData(0x90fa3ccbd60848daUL, 0x6cef866ec295abeaUL, 0xc486c0d9214beb2dUL, 0xd6e490944d5fe100UL, 0x59df3175d72c9f38UL),
            ExpectedData(0x2dbb4fc71b554514UL, 0xfcfb9443e997cabUL, 0xf13310d96dec2772UL, 0x709cad2045251af2UL, 0xafd0d30cc6376dadUL),
            ExpectedData(0xb98bf4274d18374aUL, 0x73119c99e6d508beUL, 0x5d4036a187735385UL, 0x8fa66e192fd83831UL, 0x2abf64b6b592ed57UL),
            ExpectedData(0xd6781d0b5e18eb68UL, 0xaafcb77497b5a20bUL, 0x411819e5e79b77a3UL, 0xbd779579c51c77ceUL, 0x58d11f5dcf5d075dUL),
            ExpectedData(0x226651cf18f4884cUL, 0x3f44f873be4812ecUL, 0x427662c1dbfaa7b2UL, 0xa207ff9638fb6558UL, 0xa738d919e45f550fUL),
            ExpectedData(0xa734fb047d3162d6UL, 0xd396a297799c24a1UL, 0x8fee992e3069bad5UL, 0x2e3a01b0697ccf57UL, 0xee9c7390bd901cfaUL),
            ExpectedData(0xc6df6364a24f75a3UL, 0x895fe8443183da74UL, 0xc7f2f6f895a67334UL, 0xa0d6b6a506691d31UL, 0x24f51712b459a9f0UL),
            ExpectedData(0xd8d1364c1fbcd10UL, 0xa3d5d1137d30c4bdUL, 0x1e7d706a49bdfb9eUL, 0xc63282b20ad86db2UL, 0xaec97fa07916bfd6UL),
            ExpectedData(0xaae06f9146db885fUL, 0xb22bf08d9f8aecf7UL, 0xc182730de337b922UL, 0x2b9adc87a0450a46UL, 0x192c29a9cfc00aadUL),
            ExpectedData(0x8955ef07631e3bccUL, 0x882efc2561715a9cUL, 0xef8132a18a540221UL, 0xb20a3c87a8c257c1UL, 0xf541b8628fad6c23UL),
            ExpectedData(0xad611c609cfbe412UL, 0x371a98b2cb084883UL, 0x33a2886ee9f00663UL, 0xbe9568818ed6e6bdUL, 0xf244a0fa2673469aUL),
            ExpectedData(0xd5339adc295d5d69UL, 0x89f3aab99afbd636UL, 0xf420e004f8148b9aUL, 0x6818073faa797c7cUL, 0xdd3b4e21cbbf42caUL),
            ExpectedData(0x40d0aeff521375a8UL, 0x21c2be098327f49bUL, 0x7e035065ac7bbef5UL, 0x6d7348e63023fb35UL, 0x9d427dc1b67c3830UL),
            ExpectedData(0x8b2d54ae1a3df769UL, 0x9d097dd3152ab107UL, 0x51e21d24126e8563UL, 0xcba56cac884a1354UL, 0x39abb1b595f0a977UL),
            ExpectedData(0x99c175819b4eae28UL, 0xc1a78b82ba815b74UL, 0x458cbdfc82eb322aUL, 0x17f4a192376ed8d7UL, 0x6f9e92968bc8ccefUL),
            ExpectedData(0x2a418335779b82fcUL, 0x5aeead8d6cb25bb9UL, 0x739315f7743ec3ffUL, 0x9ab48d27111d2dccUL, 0x5b87bd35a975929bUL),
            ExpectedData(0x3b1fc6a3d279e67dUL, 0xba1ffba29f0367aaUL, 0xa20bec1dd15a8b6cUL, 0xe9bf61d2dab0f774UL, 0xf4f35bf5870a049cUL),
            ExpectedData(0xd97eacdf10f1c3c9UL, 0xd8ad7ec84a9c9aa2UL, 0xe256cffed11f69e6UL, 0x2cf65e4958ad5bdaUL, 0xcfbf9b03245989a7UL),
            ExpectedData(0x293a5c1c4e203cd4UL, 0x361e0a62c8187bffUL, 0x6089971bb84d7133UL, 0x93df7741588dd50bUL, 0xc2a9b6abcd1d80b1UL),
            ExpectedData(0x4290e018ffaedde7UL, 0x4ec02f3d2f2b23f2UL, 0xab3580708aa7c339UL, 0xcdce066fbab3f65UL, 0xd8ed3ecf3c7647b9UL),
            ExpectedData(0xf919a59cbde8bf2fUL, 0xc2c9fc637dbdfcfaUL, 0x292ab8306d149d75UL, 0x7f436b874b9ffc07UL, 0xa5b56b0129218b80UL),
            ExpectedData(0x1d70a3f5521d7fa4UL, 0xe1a8286a7d67946eUL, 0x52bd956f047b298UL, 0xcbd74332dd4204acUL, 0x12b5be7752721976UL),
            ExpectedData(0x6af98d7b656d0d7cUL, 0xbde51033ac0413f8UL, 0xbc0272f691aec629UL, 0x6204332651bebc44UL, 0x1cbf00de026ea9bdUL),
            ExpectedData(0x395b7a8adb96ab75UL, 0x6c71064996cbec8bUL, 0x352c535edeefcb89UL, 0xac7f0aba15cd5ecdUL, 0x3aba1ca8353e5c60UL),
            ExpectedData(0x3822dd82c7df012fUL, 0x43e47bd5bab1e0efUL, 0x4a71f363421f282fUL, 0x880b2f32a2b4e289UL, 0x1299d4eda9d3eadfUL),
            ExpectedData(0x79f7efe4a80b951aUL, 0x832954ec9d0de333UL, 0x94c390aa9bcb6b8aUL, 0xf3b32afdc1f04f82UL, 0xd229c3b72e4b9a74UL),
            ExpectedData(0xae6e59f5f055921aUL, 0x4960111789727567UL, 0x149b8a37c7125ab6UL, 0x78c7a13ab9749382UL, 0x1c61131260ca151aUL),
            ExpectedData(0x8959dbbf07387d36UL, 0x6566d74954986ba5UL, 0x99d5235cc82519a7UL, 0x257a23805c2d825UL, 0xad75ccb968e93403UL),
            ExpectedData(0x4739613234278a49UL, 0xc8a2827404991402UL, 0x7ee5e78550f02675UL, 0x2ec53952db5ac662UL, 0x1526405a9df6794bUL),
            ExpectedData(0x420e6c926bc54841UL, 0x3edbc10e4bfee91bUL, 0xf0d681304c28ef68UL, 0x77ea602029aaaf9cUL, 0x90f070bd24c8483cUL),
            ExpectedData(0xc8601bab561bc1b7UL, 0x83707730cad725d4UL, 0xc9ca88c3a779674aUL, 0xe1c696fbbd9aa933UL, 0x723f3baab1c17a45UL),
            ExpectedData(0xb2d294931a0e20ebUL, 0x1ef8e98e1ea57269UL, 0x5971116272f45a8bUL, 0x187ad68ce95d8eacUL, 0xe94e93ee4e8ecaa6UL),
            ExpectedData(0x7966f53c37b6c6d7UL, 0x3eeb60c3f5f8143dUL, 0xa25aec05c422a24fUL, 0xb026b03ad3cca4dbUL, 0xe6e030028cc02a02UL),
            ExpectedData(0xbe9bb0abd03b7368UL, 0x36a8d13a2cbb0939UL, 0x254ac73907413230UL, 0x73520d1522315a70UL, 0x8c9fdb5cf1e1a507UL),
            ExpectedData(0xa08d128c5f1649beUL, 0x5b2b7ca856fad1c3UL, 0x8093022d682e375dUL, 0xea5d163ba7ea231fUL, 0xd6181d012c0de641UL),
            ExpectedData(0x7c386f0ffe0465acUL, 0x48b218e3b721810dUL, 0xd3757ac8609bc7fcUL, 0x111ba02a88aefc8UL, 0xe86343137d3bfc2aUL),
            ExpectedData(0xbb362094e7ef4f8UL, 0x15747d8c505ffd00UL, 0x438a15f391312cd6UL, 0xe46ca62c26d821f5UL, 0xbe78d74c9f79cb44UL),
            ExpectedData(0xcd80dea24321eea4UL, 0xd9ccef1d4be46988UL, 0x5ede0c4e383a5e66UL, 0xda69683716a54d1eUL, 0xbfc3fdf02d242d24UL),
            ExpectedData(0xd599a04125372c3aUL, 0x2870a99c76a587a4UL, 0x99f74cc0b182dda4UL, 0x8a5e895b2f0ca7b6UL, 0x3d78882d5e0bb1dcUL),
            ExpectedData(0xdbbf541e9dfda0aUL, 0xa3335c417687cf3aUL, 0x92ff114ac45cda75UL, 0xc3b8a627384f13b5UL, 0xc4f25de33de8b3f7UL),
            ExpectedData(0xc2ee3288be4fe2bfUL, 0xc7cd48f7abf1fe59UL, 0xce600656ace6f53aUL, 0x8a94a4381b108b34UL, 0xf9d1276c64bf59fbUL),
            ExpectedData(0xd86603ced1ed4730UL, 0xd803e1eead47604cUL, 0xad00f7611970a71bUL, 0xbc50036b16ce71f5UL, 0xafba96210a2ca7d6UL),
            ExpectedData(0x915263c671b28809UL, 0xd17c928c5342477fUL, 0x745130b795254ad5UL, 0x8c5db926fe88f8baUL, 0x742a95c953e6d974UL),
            ExpectedData(0x2b67cdd38c307a5eUL, 0x6531c1fe32bcb417UL, 0x8c970d8df8cdbeb4UL, 0x917ba5fc67e72b40UL, 0x4b65e4e263e0a426UL),
            ExpectedData(0x2d107419073b9cd0UL, 0xffe319654c8e7ebcUL, 0x6a67b8f13ead5a72UL, 0x6dd10a34f80d532fUL, 0x6e9cfaece9fbca4UL),
            ExpectedData(0xf3e9487ec0e26dfcUL, 0x8950cfcf4bdf622cUL, 0x8847dca82efeef2fUL, 0x646b75b026708169UL, 0x21cab4b1687bd8bUL),
            ExpectedData(0x1160987c8fe86f7dUL, 0x14453b5cc3d82396UL, 0x4ef700c33ed278bcUL, 0x1639c72ffc00d12eUL, 0xfb140ee6155f700dUL),
            ExpectedData(0xeab8112c560b967bUL, 0x276aa37744b5a028UL, 0x8c10800ee90ea573UL, 0xe6e57d2b33a1e0b7UL, 0x91f83563cd3b9ddaUL),
            ExpectedData(0x1addcf0386d35351UL, 0xff5c03f003c1fefeUL, 0xe1098670afe7ff6UL, 0xea445030cf86de19UL, 0xf155c68b5c2967f8UL),
            ExpectedData(0xd445ba84bf803e09UL, 0xe2164451c651adfbUL, 0xb2534e65477f9823UL, 0x4d70691a69671e34UL, 0x15be4963dbde8143UL),
            ExpectedData(0x37235a096a8be435UL, 0xad159f542d81f04eUL, 0x49626a97a946096UL, 0xd8d3998bf09fd304UL, 0xd127a411eae69459UL),
            ExpectedData(0x763ad6ea2fe1c99dUL, 0x3712eb913d04e2f2UL, 0x2f9500d319c84d89UL, 0x4ac6eb21a8cf06f9UL, 0x7d1917afcde42744UL),
            ExpectedData(0xea627fc84cd1b857UL, 0xa3c1c5ca1b0367UL, 0xeb6933997272bb3dUL, 0x76a72cb62692a655UL, 0x140bb5531edf756eUL),
            ExpectedData(0x1f2ffd79f2cdc0c8UL, 0x5aa82bfaa99d3978UL, 0xc18f96cade5ce18dUL, 0x38404491f9e34c03UL, 0x891fb8926ba0418cUL),
            ExpectedData(0x39a9e146ec4b3210UL, 0x8b305d532e61226eUL, 0xcaeae80da2ea2eUL, 0x88a6289a76ac684eUL, 0x8ce5b5f9df1cbd85UL),
            ExpectedData(0x74cba303e2dd9d6dUL, 0x751390a8a5c41bdcUL, 0x6ee5fbf87605d34UL, 0x6ca73f610f3a8f7cUL, 0xe898b3c996570adUL),
            ExpectedData(0x4cbc2b73a43071e0UL, 0xb87a326e413604bfUL, 0xd8f9a5fa214b03abUL, 0x8a8bb8265771cf88UL, 0xa655319054f6e70fUL),
            ExpectedData(0x875638b9715d2221UL, 0x5df25f13ea7bc284UL, 0x165edfaafd2598fbUL, 0xaf7215c5c718c696UL, 0xe9f2f9ca655e769UL),
            ExpectedData(0xfb686b2782994a8dUL, 0x58eb4d03b2c3ddf5UL, 0x6d2542995f9189f1UL, 0xc0beec58a5f5fea2UL, 0xed67436f42e2a78bUL),
            ExpectedData(0xab21d81a911e6723UL, 0x7f759dddc6e8549aUL, 0x616dd0ca022c8735UL, 0x94717ad4bc15ceb3UL, 0xf66c7be808ab36eUL),
            ExpectedData(0x33d013cc0cd46ecfUL, 0xf271ba474edc562dUL, 0xe6596e67f9dd3ebdUL, 0xc0a288edf808f383UL, 0xb3def70681c6babcUL),
            ExpectedData(0x8ca92c7cd39fae5dUL, 0x45744afcf131dbeeUL, 0x97222392c2559350UL, 0x498a19b280c6d6edUL, 0x83ac2c36acdb8d49UL),
            ExpectedData(0xfdde3b03f018f43eUL, 0xb6dd09ba7851c7afUL, 0x570de4e1bb13b133UL, 0xc4e784eb97211642UL, 0x8285a7fcdcc7c58dUL),
            ExpectedData(0x9c8502050e9c9458UL, 0x216e1d6c86cb524cUL, 0xd01cf6fd4f4065c0UL, 0xfffa4ec5b482ea0fUL, 0xa0e20ee6a5404ac1UL),
            ExpectedData(0x348176ca2fa2fdd2UL, 0xbceee07c11a9ac30UL, 0x2e2d47dff8e77eb7UL, 0x11a394cd7b6d614aUL, 0x1d7c41d54e15cb4aUL),
            ExpectedData(0x4a3d3dfbbaea130bUL, 0xbd2b31b5608143feUL, 0xab717a10f2554853UL, 0x293857f04d194d22UL, 0xd51be8fa86f254f0UL),
            ExpectedData(0xb371f768cdf4edb9UL, 0xb9e0d415b4ebd534UL, 0xc97c2a27efaa33d7UL, 0x591cdb35f84ef9daUL, 0xa57d02d0e8e3756cUL),
            ExpectedData(0x7a1d2e96934f61fUL, 0x2228d6725e31b8abUL, 0x9b98f7e4d0142e70UL, 0xb6a8c2115b8e0fe7UL, 0xb591e2f5ab9b94b1UL),
            ExpectedData(0x8be53d466d4728f2UL, 0x87049e68f5d38e59UL, 0x7d8ce44ec6bd7751UL, 0xcc28d08ab414839cUL, 0x6c8f0bd34fe843e3UL),
            ExpectedData(0x829677eb03abf042UL, 0x98d0dbf796480187UL, 0xfbcb5f3e1bef5742UL, 0x5af2a0463bf6e921UL, 0xad9555bf0120b3a3UL),
            ExpectedData(0x754435bae3496fcUL, 0x57c5208e8f021a77UL, 0xf7653fbb69cd9276UL, 0xa484410af21d75cbUL, 0xf19b6844b3d627e8UL),
            ExpectedData(0xfda9877ea8e3805fUL, 0x68110a7f83f5d3ffUL, 0x6d77e045901b85a8UL, 0x84ef681113036d8bUL, 0x3b9f8e3928f56160UL),
            ExpectedData(0x2e36f523ca8f5eb5UL, 0xd1bfe4df12b04cbfUL, 0xf58c17243fd63842UL, 0x3a453cdba80a60afUL, 0x5737b2ca7470ea95UL),
            ExpectedData(0x21a378ef76828208UL, 0x61c9c95d91017da5UL, 0x16f7c83ba68f5279UL, 0x9c0619b0808d05f7UL, 0x83c117ce4e6b70a3UL),
            ExpectedData(0xccdd5600054b16caUL, 0x58634004c7b2d19aUL, 0x24bb5f51ed3b9073UL, 0x46409de018033d00UL, 0x4a9805eed5ac802eUL),
            ExpectedData(0x7854468f4e0cabd0UL, 0x29c3529eb165eebaUL, 0x443de3703b657c35UL, 0x66acbce31ae1bc8dUL, 0x1acc99effe1d547eUL),
            ExpectedData(0x7f88db5346d8f997UL, 0xae59ca86f4c3323dUL, 0x25906c09906d5c4cUL, 0x8dd2aa0c0a6584aeUL, 0x232a7d96b38f40e9UL),
            ExpectedData(0xbb3fb5fb01d60fcfUL, 0xd4edc954c07cd8f3UL, 0x224f47e7c00a30abUL, 0xd5ad7ad7f41ef0c6UL, 0x59e089281d869fd7UL),
            ExpectedData(0x2e783e1761acd84dUL, 0xb1b7ec44f9302176UL, 0x5cb476450dc0c297UL, 0xdc5ef652521ef6a2UL, 0x3cc79a9e334e1f84UL),
            ExpectedData(0x392058251cf22accUL, 0x54bc9bee7cbe1767UL, 0x485820bdbe442431UL, 0x54d6120ea2972e90UL, 0xf437a0341f29b72aUL),
            ExpectedData(0xadf5c1e5d6419947UL, 0x80973ea532b0f310UL, 0xa471829aa9c17dd9UL, 0xc2ff3479394804abUL, 0x6bf44f8606753636UL),
            ExpectedData(0x6bc1db2c2bee5abaUL, 0x230d2b3e47f09830UL, 0xec8624a821c1caf4UL, 0xea6ec411cdbf1cb1UL, 0x5f38ae82af364e27UL),
            ExpectedData(0xb00f898229efa508UL, 0x7122413bdbc94035UL, 0xe7f90fae33bf7763UL, 0x4b6bd0fb30b12387UL, 0x557359c0c44f48caUL),
            ExpectedData(0xb56eb769ce0d9a8cUL, 0x5ed12338f630ab76UL, 0xfab19fcb319116dUL, 0x167f5f42b521724bUL, 0xc4aa56c409568d74UL),
            ExpectedData(0x70c0637675b94150UL, 0xfca4e5bc9292788eUL, 0xcd509dc1facce41cUL, 0xbbba575a59d82feUL, 0x4e2e71c15b45d4d3UL),
            ExpectedData(0x74c0b8a6821faafeUL, 0x967e970df9673d2aUL, 0xd465247cffa415c0UL, 0x33a1df0ca1107722UL, 0x49fc2a10adce4a32UL),
            ExpectedData(0x5fb5e48ac7b7fa4fUL, 0x6cc09e60700563e9UL, 0xd18f23221e964791UL, 0xffc23eeef7af26ebUL, 0x693a954a3622a315UL)
        )

        var a = 9UL
        var b = 777UL

        val data = ByteArray(1 shl 20) {
            a += b
            b += a
            a = (a xor (a shr 41)) * k0
            b = (b xor (b shr 41)) * k0 + it.toULong()
            (b shr 37).toByte()
        }

        var i = 0
        while (i < testData.size - 1) {
            testData[i].doTest(data, i * i, i)
            i++
        }
        testData[i].doTest(data, 0, data.size)
    }
}
