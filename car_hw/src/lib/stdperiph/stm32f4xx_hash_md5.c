/**
  ******************************************************************************
  * @file    stm32f4xx_hash_md5.c
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file provides high level functions to compute the HASH MD5 and
  *          HMAC MD5 Digest of an input message.
  *          It uses the stm32f4xx_hash.c/.h drivers to access the STM32F4xx HASH
  *          peripheral.
  *
  *  @verbatim
  *    
  *          ===================================================================
  *                                   How to use this driver
  *          ===================================================================
  *          1. Enable The HASH controller clock using 
  *            RCC_AHB2PeriphClockCmd(RCC_AHB2Periph_HASH, ENABLE); function.
  *
  *          2. Calculate the HASH MD5 Digest using HASH_MD5() function.
  *
  *          3. Calculate the HMAC MD5 Digest using HMAC_MD5() function.
  *
  *  @endverbatim
  *
  ******************************************************************************
  * @attention
  *
  * THE PRESENT FIRMWARE WHICH IS FOR GUIDANCE ONLY AIMS AT PROVIDING CUSTOMERS
  * WITH CODING INFORMATION REGARDING THEIR PRODUCTS IN ORDER FOR THEM TO SAVE
  * TIME. AS A RESULT, STMICROELECTRONICS SHALL NOT BE HELD LIABLE FOR ANY
  * DIRECT, INDIRECT OR CONSEQUENTIAL DAMAGES WITH RESPECT TO ANY CLAIMS ARISING
  * FROM THE CONTENT OF SUCH FIRMWARE AND/OR THE USE MADE BY CUSTOMERS OF THE
  * CODING INFORMATION CONTAINED HEREIN IN CONNECTION WITH THEIR PRODUCTS.
  *
  * <h2><center>&copy; COPYRIGHT 2011 STMicroelectronics</center></h2>
  ******************************************************************************
  */

/* Includes ------------------------------------------------------------------*/
#include "stm32f4xx_hash.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @defgroup HASH 
  * @brief HASH driver modules
  * @{
  */

/* Private typedef -----------------------------------------------------------*/
/* Private define ------------------------------------------------------------*/
#define MD5BUSY_TIMEOUT    ((uint32_t) 0x00010000)

/* Private macro -------------------------------------------------------------*/
/* Private variables ---------------------------------------------------------*/
/* Private function prototypes -----------------------------------------------*/
/* Private functions ---------------------------------------------------------*/

/** @defgroup HASH_Private_Functions
  * @{
  */ 

/** @defgroup HASH_Group7 High Level MD5 functions
 *  @brief   High Level MD5 Hash and HMAC functions 
 *
@verbatim   
 ===============================================================================
                          High Level MD5 Hash and HMAC functions
 ===============================================================================


@endverbatim
  * @{
  */

/**
  * @brief  Compute the HASH MD5 digest.
  * @param  Input: pointer to the Input buffer to be treated.
  * @param  Ilen: length of the Input buffer.
  * @param  Output: the returned digest
  * @retval An ErrorStatus enumeration value:
  *          - SUCCESS: digest computation done
  *          - ERROR: digest computation failed
  */
ErrorStatus HASH_MD5(uint8_t *Input, uint32_t Ilen, uint8_t Output[16])
{
  HASH_InitTypeDef MD5_HASH_InitStructure;
  HASH_MsgDigest MD5_MessageDigest;
  __IO uint16_t nbvalidbitsdata = 0;
  uint32_t i = 0;
  __IO uint32_t counter = 0;
  uint32_t busystatus = 0;
  ErrorStatus status = SUCCESS;
  uint32_t inputaddr  = (uint32_t)Input;
  uint32_t outputaddr = (uint32_t)Output;


  /* Number of valid bits in last word of the Input data */
  nbvalidbitsdata = 8 * (Ilen % 4);

  /* HASH peripheral initialization */
  HASH_DeInit();

  /* HASH Configuration */
  MD5_HASH_InitStructure.HASH_AlgoSelection = HASH_AlgoSelection_MD5;
  MD5_HASH_InitStructure.HASH_AlgoMode = HASH_AlgoMode_HASH;
  MD5_HASH_InitStructure.HASH_DataType = HASH_DataType_8b;
  HASH_Init(&MD5_HASH_InitStructure);

  /* Configure the number of valid bits in last word of the data */
  HASH_SetLastWordValidBitsNbr(nbvalidbitsdata);

  /* Write the Input block in the IN FIFO */
  for(i=0; i<Ilen; i+=4)
  {
    HASH_DataIn(*(uint32_t*)inputaddr);
    inputaddr+=4;
  }

  /* Start the HASH processor */
  HASH_StartDigest();

  /* wait until the Busy flag is RESET */
  do
  {
    busystatus = HASH_GetFlagStatus(HASH_FLAG_BUSY);
    counter++;
  }while ((counter != MD5BUSY_TIMEOUT) && (busystatus != RESET));

  if (busystatus != RESET)
  {
     status = ERROR;
  }
  else
  {
    /* Read the message digest */
    HASH_GetDigest(&MD5_MessageDigest);
    *(uint32_t*)(outputaddr)  = __REV(MD5_MessageDigest.Data[0]);
    outputaddr+=4;
    *(uint32_t*)(outputaddr)  = __REV(MD5_MessageDigest.Data[1]);
    outputaddr+=4;
    *(uint32_t*)(outputaddr)  = __REV(MD5_MessageDigest.Data[2]);
    outputaddr+=4;
    *(uint32_t*)(outputaddr)  = __REV(MD5_MessageDigest.Data[3]);
  }
  return status; 
}

/**
  * @brief  Compute the HMAC MD5 digest.
  * @param  Key: pointer to the Key used for HMAC.
  * @param  Keylen: length of the Key used for HMAC.
  * @param  Input: pointer to the Input buffer to be treated.
  * @param  Ilen: length of the Input buffer.
  * @param  Output: the returned digest  
  * @retval An ErrorStatus enumeration value:
  *          - SUCCESS: digest computation done
  *          - ERROR: digest computation failed
  */
ErrorStatus HMAC_MD5(uint8_t *Key, uint32_t Keylen, uint8_t *Input, 
                     uint32_t Ilen, uint8_t Output[16])
{
  HASH_InitTypeDef MD5_HASH_InitStructure;
  HASH_MsgDigest MD5_MessageDigest;
  __IO uint16_t nbvalidbitsdata = 0;
  __IO uint16_t nbvalidbitskey = 0;
  uint32_t i = 0;
  __IO uint32_t counter = 0;
  uint32_t busystatus = 0;
  ErrorStatus status = SUCCESS;
  uint32_t keyaddr    = (uint32_t)Key;
  uint32_t inputaddr  = (uint32_t)Input;
  uint32_t outputaddr = (uint32_t)Output;

  /* Number of valid bits in last word of the Input data */
  nbvalidbitsdata = 8 * (Ilen % 4);

  /* Number of valid bits in last word of the Key */
  nbvalidbitskey = 8 * (Keylen % 4);
   
  /* HASH peripheral initialization */
  HASH_DeInit();

  /* HASH Configuration */
  MD5_HASH_InitStructure.HASH_AlgoSelection = HASH_AlgoSelection_MD5;
  MD5_HASH_InitStructure.HASH_AlgoMode = HASH_AlgoMode_HMAC;
  MD5_HASH_InitStructure.HASH_DataType = HASH_DataType_8b;
  if(Keylen > 64)
  {
    /* HMAC long Key */
    MD5_HASH_InitStructure.HASH_HMACKeyType = HASH_HMACKeyType_LongKey;
  }
  else
  {
    /* HMAC short Key */
    MD5_HASH_InitStructure.HASH_HMACKeyType = HASH_HMACKeyType_ShortKey;
  }
  HASH_Init(&MD5_HASH_InitStructure);

  /* Configure the number of valid bits in last word of the Key */
  HASH_SetLastWordValidBitsNbr(nbvalidbitskey);

  /* Write the Key */
  for(i=0; i<Keylen; i+=4)
  {
    HASH_DataIn(*(uint32_t*)keyaddr);
    keyaddr+=4;
  }
  
  /* Start the HASH processor */
  HASH_StartDigest();

  /* wait until the Busy flag is RESET */
  do
  {
    busystatus = HASH_GetFlagStatus(HASH_FLAG_BUSY);
    counter++;
  }while ((counter != MD5BUSY_TIMEOUT) && (busystatus != RESET));

  if (busystatus != RESET)
  {
     status = ERROR;
  }
  else
  {
    /* Configure the number of valid bits in last word of the Input data */
    HASH_SetLastWordValidBitsNbr(nbvalidbitsdata);

    /* Write the Input block in the IN FIFO */
    for(i=0; i<Ilen; i+=4)
    {
      HASH_DataIn(*(uint32_t*)inputaddr);
      inputaddr+=4;
    }

    /* Start the HASH processor */
    HASH_StartDigest();

    /* wait until the Busy flag is RESET */
    counter =0;
    do
    {
       busystatus = HASH_GetFlagStatus(HASH_FLAG_BUSY);
       counter++;
    }while ((counter != MD5BUSY_TIMEOUT) && (busystatus != RESET));

    if (busystatus != RESET)
    {
      status = ERROR;
    }
    else
    {  
      /* Configure the number of valid bits in last word of the Key */
      HASH_SetLastWordValidBitsNbr(nbvalidbitskey);

      /* Write the Key */
      keyaddr = (uint32_t)Key;
      for(i=0; i<Keylen; i+=4)
      {
        HASH_DataIn(*(uint32_t*)keyaddr);
        keyaddr+=4;
      }
  
       /* Start the HASH processor */
       HASH_StartDigest();

       /* wait until the Busy flag is RESET */
       counter =0;
       do
       {
          busystatus = HASH_GetFlagStatus(HASH_FLAG_BUSY);
          counter++;
      }while ((counter != MD5BUSY_TIMEOUT) && (busystatus != RESET));

      if (busystatus != RESET)
      {
         status = ERROR;
      }
      else
      {
         /* Read the message digest */
         HASH_GetDigest(&MD5_MessageDigest);
         *(uint32_t*)(outputaddr)  = __REV(MD5_MessageDigest.Data[0]);
         outputaddr+=4;
         *(uint32_t*)(outputaddr)  = __REV(MD5_MessageDigest.Data[1]);
         outputaddr+=4;
         *(uint32_t*)(outputaddr)  = __REV(MD5_MessageDigest.Data[2]);
         outputaddr+=4;
         *(uint32_t*)(outputaddr)  = __REV(MD5_MessageDigest.Data[3]);
      }
    }
  }
  return status;  
}
/**
  * @}
  */ 

/**
  * @}
  */ 

/**
  * @}
  */ 

/**
  * @}
  */ 

/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/

